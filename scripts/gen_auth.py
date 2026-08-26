# -*- coding: utf-8 -*-
from pathlib import Path
ROOT = Path(r"E:\aseantec\agent\Smart Fitness")

def w(rel, text):
    p = ROOT / rel.replace("/", "\\")
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text.lstrip("\n"), encoding="utf-8")
    print(rel)

# ===== AUTH =====
w("smart-fitness-auth/src/main/java/com/aseantec/smartfitness/auth/entity/AppUser.java", """
package com.aseantec.smartfitness.auth.entity;

import com.aseantec.smartfitness.common.entity.AuditedEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_user")
public class AppUser extends AuditedEntity {
    private String email;
    private String phone;
    private String passwordHash;
    private String status;
}
""")

w("smart-fitness-auth/src/main/java/com/aseantec/smartfitness/auth/mapper/AppUserMapper.java", """
package com.aseantec.smartfitness.auth.mapper;

import com.aseantec.smartfitness.auth.entity.AppUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AppUserMapper extends BaseMapper<AppUser> {
}
""")

w("smart-fitness-auth/src/main/java/com/aseantec/smartfitness/auth/config/JwtProperties.java", """
package com.aseantec.smartfitness.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret = "change-me-to-at-least-32-bytes-long!!";
    private String adminSecret = "change-me-admin-secret-32-bytes-min";
    private long accessTtlSeconds = 7200;
    private long refreshTtlSeconds = 1209600;
}
""")

w("smart-fitness-auth/src/main/java/com/aseantec/smartfitness/auth/dto/RegisterRequest.java", """
package com.aseantec.smartfitness.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    @Email
    private String email;
    private String phone;
    @NotBlank
    @Size(min = 8, max = 64)
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\\\d).+$", message = "password must contain letter and digit")
    private String password;
}
""")

w("smart-fitness-auth/src/main/java/com/aseantec/smartfitness/auth/dto/LoginRequest.java", """
package com.aseantec.smartfitness.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank
    private String account;
    @NotBlank
    private String password;
}
""")

w("smart-fitness-auth/src/main/java/com/aseantec/smartfitness/auth/dto/RefreshRequest.java", """
package com.aseantec.smartfitness.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshRequest {
    @NotBlank
    private String refreshToken;
}
""")

w("smart-fitness-auth/src/main/java/com/aseantec/smartfitness/auth/vo/TokenVO.java", """
package com.aseantec.smartfitness.auth.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TokenVO {
    String accessToken;
    String refreshToken;
    long expiresIn;
    String athleteId;
    boolean onboarded;
}
""")

w("smart-fitness-auth/src/main/java/com/aseantec/smartfitness/auth/service/JwtService.java", r'''
package com.aseantec.smartfitness.auth.service;

import com.aseantec.smartfitness.auth.config.JwtProperties;
import com.aseantec.smartfitness.common.context.CurrentUser;
import com.aseantec.smartfitness.common.exception.BizException;
import com.aseantec.smartfitness.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtProperties properties;

    public String createAccessToken(Long userId, Long athleteId, String audience) {
        return build(userId, athleteId, audience, "access", properties.getAccessTtlSeconds());
    }

    public String createRefreshToken(Long userId, Long athleteId, String audience) {
        return build(userId, athleteId, audience, "refresh", properties.getRefreshTtlSeconds());
    }

    public CurrentUser parse(String token, boolean admin) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key(admin))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!"access".equals(claims.get("typ", String.class))) {
                throw new BizException(ErrorCode.UNAUTHORIZED, "not an access token");
            }
            Long userId = Long.valueOf(claims.getSubject());
            Object athlete = claims.get("athleteId");
            return CurrentUser.builder()
                    .userId(userId)
                    .athleteId(athlete == null ? null : Long.valueOf(athlete.toString()))
                    .audience(claims.get("aud", String.class))
                    .tokenId(claims.getId())
                    .admin(admin)
                    .build();
        } catch (ExpiredJwtException ex) {
            throw new BizException(ErrorCode.TOKEN_EXPIRED);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
    }

    public CurrentUser parseRefresh(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key(false)).build()
                    .parseSignedClaims(token).getPayload();
            if (!"refresh".equals(claims.get("typ", String.class))) {
                throw new BizException(ErrorCode.UNAUTHORIZED, "not a refresh token");
            }
            Object athlete = claims.get("athleteId");
            return CurrentUser.builder()
                    .userId(Long.valueOf(claims.getSubject()))
                    .athleteId(athlete == null ? null : Long.valueOf(athlete.toString()))
                    .audience("app")
                    .tokenId(claims.getId())
                    .admin(false)
                    .build();
        } catch (ExpiredJwtException ex) {
            throw new BizException(ErrorCode.TOKEN_EXPIRED);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
    }

    public long accessTtlSeconds() {
        return properties.getAccessTtlSeconds();
    }

    private String build(Long userId, Long athleteId, String audience, String typ, long ttl) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim("typ", typ)
                .claim("aud", audience)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttl)))
                .signWith(key("admin".equals(audience)));
        if (athleteId != null) {
            builder.claim("athleteId", athleteId.toString());
        }
        return builder.compact();
    }

    private SecretKey key(boolean admin) {
        String secret = admin ? properties.getAdminSecret() : properties.getSecret();
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
''')

print("auth part1 ok")
