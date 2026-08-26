package com.aseantec.smartfitness.infra.usage.mapper;

import com.aseantec.smartfitness.infra.usage.entity.LlmCallUsage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LlmCallUsageMapper extends BaseMapper<LlmCallUsage> {
}
