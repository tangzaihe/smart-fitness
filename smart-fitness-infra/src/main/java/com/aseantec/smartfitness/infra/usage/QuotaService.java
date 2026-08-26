package com.aseantec.smartfitness.infra.usage;

import com.aseantec.smartfitness.common.exception.BizException;
import com.aseantec.smartfitness.common.exception.ErrorCode;
import com.aseantec.smartfitness.common.constant.RedisKeys;
import com.aseantec.smartfitness.infra.usage.entity.LlmQuota;
import com.aseantec.smartfitness.infra.usage.mapper.LlmQuotaMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class QuotaService {
    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final LlmQuotaMapper quotaMapper;
    private final RedissonClient redissonClient;

    public LlmQuota resolveSystemQuota(Long athleteId) {
        LlmQuota personal = quotaMapper.selectOne(new LambdaQueryWrapper<LlmQuota>()
                .eq(LlmQuota::getAthleteId, athleteId)
                .eq(LlmQuota::getPeriod, "DAY")
                .last("LIMIT 1"));
        if (personal != null) {
            return personal;
        }
        LlmQuota global = quotaMapper.selectOne(new LambdaQueryWrapper<LlmQuota>()
                .isNull(LlmQuota::getAthleteId)
                .eq(LlmQuota::getPeriod, "DAY")
                .last("LIMIT 1"));
        if (global == null) {
            throw new BizException(ErrorCode.SYSTEM, "system quota is not seeded");
        }
        return global;
    }

    public void assertCanCall(Long athleteId) {
        LlmQuota quota = resolveSystemQuota(athleteId);
        String minute = java.time.OffsetDateTime.now(ZoneOffset.UTC).format(MINUTE);
        RAtomicLong rpm = redissonClient.getAtomicLong(RedisKeys.rpm(athleteId, minute));
        long rpmVal = rpm.incrementAndGet();
        if (rpmVal == 1L) {
            rpm.expire(Duration.ofMinutes(2));
        }
        int rpmLimit = quota.getRpmLimit() == null ? 20 : quota.getRpmLimit();
        if (rpmVal > rpmLimit) {
            throw new BizException(ErrorCode.RPM_EXCEEDED);
        }
        long used = redissonClient.getAtomicLong(RedisKeys.systemUsage(athleteId, LocalDate.now(ZoneOffset.UTC))).get();
        long limit = quota.getTokenLimit() == null ? Long.MAX_VALUE : quota.getTokenLimit();
        if (used >= limit) {
            throw new BizException(ErrorCode.QUOTA_EXCEEDED);
        }
    }

    public void addSystemTokens(Long athleteId, int tokens) {
        if (tokens <= 0) {
            return;
        }
        LocalDate day = LocalDate.now(ZoneOffset.UTC);
        RAtomicLong counter = redissonClient.getAtomicLong(RedisKeys.systemUsage(athleteId, day));
        counter.addAndGet(tokens);
        if (counter.remainTimeToLive() < 0) {
            counter.expire(Duration.ofHours(48));
        }
    }
}
