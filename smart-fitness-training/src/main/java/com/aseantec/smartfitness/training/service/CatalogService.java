package com.aseantec.smartfitness.training.service;

import com.aseantec.smartfitness.training.entity.ExerciseCatalog;
import com.aseantec.smartfitness.training.mapper.ExerciseCatalogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogService {
    private final ExerciseCatalogMapper catalogMapper;

    public List<ExerciseCatalog> listActive() {
        return catalogMapper.selectList(new LambdaQueryWrapper<ExerciseCatalog>()
                .eq(ExerciseCatalog::getIsStretch, false));
    }

    public ExerciseCatalog requireByCode(String code) {
        ExerciseCatalog row = catalogMapper.selectOne(new LambdaQueryWrapper<ExerciseCatalog>()
                .eq(ExerciseCatalog::getCode, code).last("LIMIT 1"));
        if (row == null) {
            throw new com.aseantec.smartfitness.common.exception.BizException(
                    com.aseantec.smartfitness.common.exception.ErrorCode.BAD_REQUEST, "unknown exercise: " + code);
        }
        return row;
    }

    public Map<String, ExerciseCatalog> mapByCode() {
        return listActive().stream().collect(Collectors.toMap(ExerciseCatalog::getCode, Function.identity()));
    }
}
