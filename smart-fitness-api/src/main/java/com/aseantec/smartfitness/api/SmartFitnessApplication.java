package com.aseantec.smartfitness.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API 启动入口。组件扫描全项目；MyBatis 仅扫描各模块 {@code *.mapper} 包，
 * 避免把 {@code knowledge.port.KnowledgePort} 等业务接口误注册为 SQL Mapper。
 */
@SpringBootApplication(scanBasePackages = "com.aseantec.smartfitness")
@MapperScan(basePackages = {
        "com.aseantec.smartfitness.auth.mapper",
        "com.aseantec.smartfitness.athlete.mapper",
        "com.aseantec.smartfitness.readiness.mapper",
        "com.aseantec.smartfitness.training.mapper",
        "com.aseantec.smartfitness.coach.mapper",
        "com.aseantec.smartfitness.infra.usage.mapper"
})
public class SmartFitnessApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartFitnessApplication.class, args);
    }
}
