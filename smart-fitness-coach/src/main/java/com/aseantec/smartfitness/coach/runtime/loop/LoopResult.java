package com.aseantec.smartfitness.coach.runtime.loop;

import com.aseantec.smartfitness.coach.runtime.context.AgentContext;

import java.util.List;

/**
 * Agent Loop 执行结果。
 */
public record LoopResult(
        Status status,
        String answerText,
        List<String> planSteps,
        AgentContext finalContext,
        String pendingQuestion,
        String pendingTool,
        Object pendingArguments,
        String error) {

    public enum Status {
        COMPLETED,
        WAITING_USER,
        WAITING_CONFIRMATION,
        FAILED
    }

    public static LoopResult completed(String answerText, List<String> planSteps, AgentContext context) {
        return new LoopResult(Status.COMPLETED, answerText, planSteps, context,
                null, null, null, null);
    }

    public static LoopResult waitingUser(String question, String serializedAction) {
        return new LoopResult(Status.WAITING_USER, null, null, null,
                question, null, serializedAction, null);
    }

    public static LoopResult waitingConfirmation(String tool, Object arguments) {
        return new LoopResult(Status.WAITING_CONFIRMATION, null, null, null,
                null, tool, arguments, null);
    }

    public static LoopResult failed(String error) {
        return new LoopResult(Status.FAILED, null, null, null,
                null, null, null, error);
    }
}
