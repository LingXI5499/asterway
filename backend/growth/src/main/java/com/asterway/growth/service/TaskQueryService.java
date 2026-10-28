package com.asterway.growth.service;

import com.asterway.growth.api.TaskQueryApi;
import com.asterway.growth.mapper.TaskQueryMapper;
import com.asterway.growth.model.TaskName;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TaskQueryService implements TaskQueryApi {

    private final TaskQueryMapper mapper;

    public TaskQueryService(TaskQueryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Map<UUID, String> taskNames(UUID userId, List<UUID> taskIds) {
        if (taskIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> names = new LinkedHashMap<>();
        for (TaskName task : mapper.findNames(userId, taskIds)) {
            if (task.taskName() != null) {
                names.put(task.taskId(), task.taskName());
            }
        }
        return names;
    }
}
