package com.aseantec.smartfitness.coach.agent;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * 意图路由：P0 规则关键词分类，选 Skill。
 * <p>意图枚举：{@code CHAT | TODAY_SESSION | WORKOUT_PLANNING | REST}。
 * Phase 2 可替换为小模型分类；P0 关键词覆盖主路径即可。
 */
@Component
public class IntentRouter {

    /** 今日训练类：问「今天练什么 / 开练 / 给我排一节」。 */
    private static final List<String> TODAY_KEYS = List.of(
            "今天练", "今天练什么", "开练", "给我排一节", "排一节课", "今日训练", "今天训练", "练什么", "今天怎么练");
    /** 休息类：「今天休息 / 太累 / 不练了」。 */
    private static final List<String> REST_KEYS = List.of(
            "今天休息", "不练了", "太累", "休息一天", "今天不练", "想休息");
    /** 计划类：「制定计划 / 周期计划 / 4 周计划」。 */
    private static final List<String> PLAN_KEYS = List.of(
            "制定计划", "周期计划", "训练计划", "计划", "4周", "四周", "推拉腿", "分化");

    /**
     * 按用户文本 + 可选 chip 分类。
     *
     * @param text 用户消息
     * @param chip 可空，chip 优先级最高
     * @return 意图字符串，与 {@code Skill} 名对齐
     */
    public String classify(String text, String chip) {
        if (chip != null && !chip.isBlank()) {
            String c = chip.toLowerCase(Locale.ROOT);
            if (c.contains("today") || c.contains("session")) {
                return "TODAY_SESSION";
            }
            if (c.contains("rest")) {
                return "REST";
            }
            if (c.contains("plan")) {
                return "WORKOUT_PLANNING";
            }
        }
        String lower = text == null ? "" : text.toLowerCase(Locale.ROOT);
        if (containsAny(lower, REST_KEYS)) {
            return "REST";
        }
        if (containsAny(lower, PLAN_KEYS)) {
            return "WORKOUT_PLANNING";
        }
        if (containsAny(lower, TODAY_KEYS)) {
            return "TODAY_SESSION";
        }
        return "CHAT";
    }

    private boolean containsAny(String lower, List<String> keys) {
        for (String k : keys) {
            if (lower.contains(k)) {
                return true;
            }
        }
        return false;
    }
}
