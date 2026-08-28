package com.aseantec.smartfitness.coach.runtime.task;

import com.aseantec.smartfitness.coach.entity.AgentTask;
import com.aseantec.smartfitness.coach.mapper.AgentTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agent Task 生命周期：创建、状态迁移。
 */
@Service
@RequiredArgsConstructor
public class TaskService {

    private final AgentTaskMapper taskMapper;

    /**
     * 为用户消息创建 Task。
     */
    @Transactional
    public AgentTask create(Long athleteId, Long conversationId, Long sourceMessageId, String title) {
        AgentTask task = new AgentTask();
        task.setAthleteId(athleteId);
        task.setConversationId(conversationId);
        task.setSourceMessageId(sourceMessageId);
        task.setTitle(title == null || title.isBlank() ? "教练任务" : truncate(title, 200));
        task.setStatus(TaskState.CREATED.name());
        taskMapper.insert(task);
        return task;
    }

    @Transactional
    public void transition(AgentTask task, TaskState state) {
        task.setStatus(state.name());
        taskMapper.updateById(task);
    }

    public AgentTask require(Long taskId) {
        AgentTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("task not found: " + taskId);
        }
        return task;
    }

    private String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }
}
