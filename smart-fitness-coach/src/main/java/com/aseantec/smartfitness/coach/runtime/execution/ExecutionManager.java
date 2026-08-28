package com.aseantec.smartfitness.coach.runtime.execution;

import com.aseantec.smartfitness.coach.agent.CoachPolicyService;
import com.aseantec.smartfitness.coach.entity.AgentTask;
import com.aseantec.smartfitness.coach.entity.CoachPolicyVersion;
import com.aseantec.smartfitness.coach.entity.CoachRun;
import com.aseantec.smartfitness.coach.mapper.CoachRunMapper;
import com.aseantec.smartfitness.coach.runtime.task.TaskState;
import com.aseantec.smartfitness.coach.runtime.task.TaskService;
import com.aseantec.smartfitness.common.exception.BizException;
import com.aseantec.smartfitness.common.exception.ErrorCode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Execution 管理：以 {@code coach_run} 为持久化载体，绑定 {@link AgentTask}。
 */
@Service
@RequiredArgsConstructor
public class ExecutionManager {

    private final CoachRunMapper runMapper;
    private final CoachPolicyService policyService;
    private final TaskService taskService;

    /**
     * 为 Task 创建新的 Execution（coach_run）。
     */
    @Transactional
    public CoachRun create(AgentTask task, Long conversationId, Long messageId, String skill, String trigger) {
        assertNoRunning(task.getAthleteId());
        CoachPolicyVersion policy = policyService.requireActive();
        CoachRun run = new CoachRun();
        run.setTaskId(task.getId());
        run.setAthleteId(task.getAthleteId());
        run.setPolicyVersionId(policy.getId());
        run.setStatus("RUNNING");
        run.setExecutionState(ExecutionState.CREATED.name());
        run.setIteration(0);
        run.setTrigger(trigger == null ? "CONVERSATION" : trigger);
        run.setConversationId(conversationId);
        run.setMessageId(messageId);
        run.setSkill(skill);
        run.setKeySource("SYSTEM");
        run.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC));
        runMapper.insert(run);
        taskService.transition(task, TaskState.RUNNING);
        return run;
    }

    @Transactional
    public void updateState(CoachRun run, ExecutionState state, int iteration) {
        run.setExecutionState(state.name());
        run.setIteration(iteration);
        runMapper.updateById(run);
    }

    @Transactional
    public void complete(CoachRun run, AgentTask task, Integer tokenIn, Integer tokenOut) {
        run.setStatus("COMPLETED");
        run.setExecutionState(ExecutionState.COMPLETED.name());
        run.setTokenIn(tokenIn);
        run.setTokenOut(tokenOut);
        run.setEndedAt(OffsetDateTime.now(ZoneOffset.UTC));
        runMapper.updateById(run);
        taskService.transition(task, TaskState.COMPLETED);
    }

    @Transactional
    public void fail(CoachRun run, AgentTask task, String error) {
        run.setStatus("FAILED");
        run.setExecutionState(ExecutionState.FAILED.name());
        run.setErrorMessage(error);
        run.setEndedAt(OffsetDateTime.now(ZoneOffset.UTC));
        runMapper.updateById(run);
        taskService.transition(task, TaskState.FAILED);
    }

    @Transactional
    public void cancel(CoachRun run, AgentTask task) {
        run.setStatus("CANCELLED");
        run.setExecutionState(ExecutionState.CANCELLED.name());
        run.setEndedAt(OffsetDateTime.now(ZoneOffset.UTC));
        runMapper.updateById(run);
        taskService.transition(task, TaskState.CANCELLED);
    }

    @Transactional
    public void markWaitingUser(AgentTask task, CoachRun run, String pendingActionJson) {
        run.setExecutionState(ExecutionState.WAITING_USER.name());
        run.setPendingAction(pendingActionJson);
        runMapper.updateById(run);
        taskService.transition(task, TaskState.WAITING_USER);
    }

    @Transactional
    public void markWaitingConfirmation(AgentTask task, CoachRun run, String pendingActionJson) {
        run.setExecutionState(ExecutionState.WAITING_CONFIRMATION.name());
        run.setPendingAction(pendingActionJson);
        runMapper.updateById(run);
        taskService.transition(task, TaskState.WAITING_CONFIRMATION);
    }

    public CoachRun require(Long executionId) {
        CoachRun run = runMapper.selectById(executionId);
        if (run == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return run;
    }

    private void assertNoRunning(Long athleteId) {
        Long count = runMapper.selectCount(new LambdaQueryWrapper<CoachRun>()
                .eq(CoachRun::getAthleteId, athleteId)
                .eq(CoachRun::getStatus, "RUNNING"));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.RUN_ACTIVE);
        }
    }
}
