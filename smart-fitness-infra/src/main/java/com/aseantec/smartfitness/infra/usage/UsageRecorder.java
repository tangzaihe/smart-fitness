package com.aseantec.smartfitness.infra.usage;

import com.aseantec.smartfitness.infra.usage.entity.LlmCallUsage;
import com.aseantec.smartfitness.infra.usage.entity.LlmPriceList;
import com.aseantec.smartfitness.infra.usage.entity.LlmUsageDaily;
import com.aseantec.smartfitness.infra.usage.mapper.LlmCallUsageMapper;
import com.aseantec.smartfitness.infra.usage.mapper.LlmPriceListMapper;
import com.aseantec.smartfitness.infra.usage.mapper.LlmUsageDailyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class UsageRecorder {
    private final LlmCallUsageMapper callUsageMapper;
    private final LlmUsageDailyMapper dailyMapper;
    private final LlmPriceListMapper priceListMapper;
    private final QuotaService quotaService;

    @Transactional
    public LlmCallUsage record(LlmCallUsage row) {
        if (row.getPromptTokens() == null) {
            row.setPromptTokens(0);
        }
        if (row.getCompletionTokens() == null) {
            row.setCompletionTokens(0);
        }
        if (row.getCachedTokens() == null) {
            row.setCachedTokens(0);
        }
        if (row.getReasoningTokens() == null) {
            row.setReasoningTokens(0);
        }
        int total = row.getPromptTokens() + row.getCompletionTokens();
        row.setTotalTokens(total);
        row.setEstimatedCostMinor(estimateCostMinor(row));
        if (row.getCurrency() == null) {
            row.setCurrency("CNY");
        }
        callUsageMapper.insert(row);
        upsertDaily(row);
        if ("SYSTEM".equals(row.getKeySource()) && total > 0) {
            quotaService.addSystemTokens(row.getAthleteId(), total);
        }
        return row;
    }

    private int estimateCostMinor(LlmCallUsage row) {
        LlmPriceList price = priceListMapper.selectOne(new LambdaQueryWrapper<LlmPriceList>()
                .eq(LlmPriceList::getProvider, row.getProvider())
                .eq(LlmPriceList::getModel, row.getModel())
                .le(LlmPriceList::getEffectiveFrom, LocalDate.now(ZoneOffset.UTC))
                .orderByDesc(LlmPriceList::getEffectiveFrom)
                .last("LIMIT 1"));
        if (price == null) {
            return 0;
        }
        double in = row.getPromptTokens() / 1000.0 * price.getInputPer1kMinor();
        double out = row.getCompletionTokens() / 1000.0 * price.getOutputPer1kMinor();
        return (int) Math.round(in + out);
    }

    private void upsertDaily(LlmCallUsage row) {
        LocalDate day = row.getCreatedAt() == null
                ? LocalDate.now(ZoneOffset.UTC)
                : row.getCreatedAt().toLocalDate();
        LlmUsageDaily existing = dailyMapper.selectOne(new LambdaQueryWrapper<LlmUsageDaily>()
                .eq(LlmUsageDaily::getAthleteId, row.getAthleteId())
                .eq(LlmUsageDaily::getUsageDate, day)
                .eq(LlmUsageDaily::getKeySource, row.getKeySource())
                .last("LIMIT 1"));
        boolean fail = row.getStatus() != null && !"SUCCESS".equals(row.getStatus());
        int platform = "SYSTEM".equals(row.getKeySource()) ? nvl(row.getEstimatedCostMinor()) : 0;
        int byok = "BYOK".equals(row.getKeySource()) ? nvl(row.getEstimatedCostMinor()) : 0;
        if (existing == null) {
            LlmUsageDaily daily = new LlmUsageDaily();
            daily.setAthleteId(row.getAthleteId());
            daily.setUsageDate(day);
            daily.setKeySource(row.getKeySource());
            daily.setCallCount(1);
            daily.setFailCount(fail ? 1 : 0);
            daily.setPromptTokens(row.getPromptTokens().longValue());
            daily.setCompletionTokens(row.getCompletionTokens().longValue());
            daily.setTotalTokens(row.getTotalTokens().longValue());
            daily.setPlatformCostMinor(platform);
            daily.setByokEstCostMinor(byok);
            dailyMapper.insert(daily);
            return;
        }
        existing.setCallCount(existing.getCallCount() + 1);
        existing.setFailCount(existing.getFailCount() + (fail ? 1 : 0));
        existing.setPromptTokens(existing.getPromptTokens() + row.getPromptTokens());
        existing.setCompletionTokens(existing.getCompletionTokens() + row.getCompletionTokens());
        existing.setTotalTokens(existing.getTotalTokens() + row.getTotalTokens());
        existing.setPlatformCostMinor(existing.getPlatformCostMinor() + platform);
        existing.setByokEstCostMinor(existing.getByokEstCostMinor() + byok);
        dailyMapper.updateById(existing);
    }

    private int nvl(Integer v) {
        return v == null ? 0 : v;
    }
}
