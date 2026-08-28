package com.aseantec.smartfitness.coach.runtime.action;

/**
 * 向用户提问并挂起 Execution，等待用户补充信息。
 *
 * @param question 展示给用户的提问
 */
public record AskUserAction(String question) implements AgentAction {
}
