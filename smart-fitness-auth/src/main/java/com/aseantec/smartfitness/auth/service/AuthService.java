package com.aseantec.smartfitness.auth.service;

import com.aseantec.smartfitness.athlete.service.AthleteService;
import com.aseantec.smartfitness.auth.dto.LoginRequest;
import com.aseantec.smartfitness.auth.dto.RefreshRequest;
import com.aseantec.smartfitness.auth.dto.RegisterRequest;
import com.aseantec.smartfitness.auth.entity.AppUser;
import com.aseantec.smartfitness.auth.mapper.AppUserMapper;
import com.aseantec.smartfitness.auth.vo.TokenVO;
import com.aseantec.smartfitness.common.constant.RedisKeys;
import com.aseantec.smartfitness.common.context.CurrentUser;
import com.aseantec.smartfitness.common.exception.BizException;
import com.aseantec.smartfitness.common.exception.ErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AppUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AthleteService athleteService;
    private final RedissonClient redissonClient;

    @Transactional
    public TokenVO register(RegisterRequest request) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getEmail, request.getEmail().toLowerCase()));
        if (exists != null && exists > 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "email already registered");
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            Long phoneExists = userMapper.selectCount(new LambdaQueryWrapper<AppUser>()
                    .eq(AppUser::getPhone, request.getPhone()));
            if (phoneExists != null && phoneExists > 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "phone already registered");
            }
        }
        AppUser user = new AppUser();
        user.setEmail(request.getEmail().toLowerCase());
        user.setPhone(blankToNull(request.getPhone()));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus("ACTIVE");
        userMapper.insert(user);
        var athlete = athleteService.createStub(user.getId(), request.getEmail());
        return tokens(user.getId(), athlete.getId(), athlete.getOnboardedAt() != null);
    }

    public TokenVO login(LoginRequest request) {
        String account = request.getAccount().trim().toLowerCase();
        RAtomicLong fails = redissonClient.getAtomicLong(RedisKeys.loginFail(account));
        if (fails.get() >= 10) {
            throw new BizException(ErrorCode.LOGIN_LOCKED);
        }
        AppUser user = userMapper.selectOne(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getEmail, account)
                .or()
                .eq(AppUser::getPhone, request.getAccount().trim())
                .last("LIMIT 1"));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            long n = fails.incrementAndGet();
            if (n == 1L) {
                fails.expire(Duration.ofMinutes(15));
            }
            throw new BizException(ErrorCode.UNAUTHORIZED, "invalid credentials");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BizException(ErrorCode.FORBIDDEN, "account disabled");
        }
        fails.delete();
        var athlete = athleteService.requireByUserId(user.getId());
        return tokens(user.getId(), athlete.getId(), athlete.getOnboardedAt() != null);
    }

    public TokenVO refresh(RefreshRequest request) {
        CurrentUser user = jwtService.parseRefresh(request.getRefreshToken());
        if (isBlacklisted(user.getTokenId())) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        blacklist(user.getTokenId(), Duration.ofDays(14));
        var athlete = athleteService.requireByUserId(user.getUserId());
        return tokens(user.getUserId(), athlete.getId(), athlete.getOnboardedAt() != null);
    }

    public void logout(CurrentUser user, String refreshToken) {
        if (user.getTokenId() != null) {
            blacklist(user.getTokenId(), Duration.ofHours(2));
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                CurrentUser refresh = jwtService.parseRefresh(refreshToken);
                blacklist(refresh.getTokenId(), Duration.ofDays(14));
            } catch (BizException ignored) {
                // already invalid
            }
        }
    }

    public boolean isBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }
        try {
            return redissonClient.getBucket(RedisKeys.jwtBlacklist(jti)).isExists();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private void blacklist(String jti, Duration ttl) {
        if (jti == null) {
            return;
        }
        RBucket<String> bucket = redissonClient.getBucket(RedisKeys.jwtBlacklist(jti));
        bucket.set("1", ttl);
    }

    private TokenVO tokens(Long userId, Long athleteId, boolean onboarded) {
        return TokenVO.builder()
                .accessToken(jwtService.createAccessToken(userId, athleteId, "app"))
                .refreshToken(jwtService.createRefreshToken(userId, athleteId, "app"))
                .expiresIn(jwtService.accessTtlSeconds())
                .athleteId(String.valueOf(athleteId))
                .onboarded(onboarded)
                .build();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
