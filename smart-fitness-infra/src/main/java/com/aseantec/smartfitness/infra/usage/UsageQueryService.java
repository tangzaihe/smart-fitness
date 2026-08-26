package com.aseantec.smartfitness.infra.usage;

import com.aseantec.smartfitness.infra.usage.entity.LlmUsageDaily;
import com.aseantec.smartfitness.infra.usage.mapper.LlmUsageDailyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UsageQueryService {
    private final LlmUsageDailyMapper dailyMapper;

    public List<LlmUsageDaily> lastDays(Long athleteId, int days) {
        LocalDate from = LocalDate.now().minusDays(days - 1L);
        return dailyMapper.selectList(new LambdaQueryWrapper<LlmUsageDaily>()
                .eq(LlmUsageDaily::getAthleteId, athleteId)
                .ge(LlmUsageDaily::getUsageDate, from)
                .orderByAsc(LlmUsageDaily::getUsageDate));
    }
}
