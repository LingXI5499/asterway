package com.asterway.app.service;

import com.asterway.execution.api.ExecutionQueryApi;
import com.asterway.growth.api.TaskQueryApi;
import com.asterway.identity.api.AccountExecutionPort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// 通过模块公开查询组合账户提示，不直接读取执行或任务表。
@Service
public class AccountExecutionQueryService implements AccountExecutionPort {

    private final ExecutionQueryApi executions;
    private final TaskQueryApi tasks;

    public AccountExecutionQueryService(ExecutionQueryApi executions, TaskQueryApi tasks) {
        this.executions = executions;
        this.tasks = tasks;
    }

    @Override
    public int unfinishedCount(UUID userId) {
        return executions.unfinishedCount(userId);
    }

    @Override
    public List<Map<String, Object>> unfinishedHints(UUID userId) {
        List<Map<String, Object>> hints = executions.unfinishedHints(userId);
        List<UUID> taskIds = hints.stream()
                .map(hint -> (UUID) hint.get("taskId"))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, String> names = taskIds.isEmpty() ? Map.of() : tasks.taskNames(userId, taskIds);
        List<Map<String, Object>> result = new ArrayList<>(hints.size());
        for (Map<String, Object> hint : hints) {
            Map<String, Object> item = new LinkedHashMap<>(hint);
            UUID taskId = (UUID) hint.get("taskId");
            String taskName = taskId == null ? null : names.get(taskId);
            if (taskName != null) {
                item.put("taskName", taskName);
            }
            item.put("allowedActions", List.of("OPEN_EXECUTION", "END_EXECUTION"));
            result.add(item);
        }
        return result;
    }
}
