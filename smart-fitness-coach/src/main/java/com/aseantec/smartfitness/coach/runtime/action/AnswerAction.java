package com.aseantec.smartfitness.coach.runtime.action;

/**
 * 直接回复用户并结束当前迭代（可能随后 COMPLETE）。
 *
 * @param text 回复正文
 */
public record AnswerAction(String text) implements AgentAction {
}
