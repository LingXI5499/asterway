package com.asterway.execution.service;

import com.asterway.execution.api.ExecutionQueryApi;
import com.asterway.execution.mapper.ExecutionQueryMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ExecutionQueryService implements ExecutionQueryApi {

    private final ExecutionQueryMapper mapper;

    public ExecutionQueryService(ExecutionQueryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public int unfinishedCount(UUID userId) {
        return mapper.exists() ? mapper.countUnfinished(userId) : 0;
    }

    @Override
    public List<Map<String, Object>> unfinishedHints(UUID userId) {
        // 执行表尚未建立时沿用没有未结束执行的读取语义。
        if (!mapper.exists()) {
            return List.of();
        }
        return mapper.findUnfinishedHints(userId);
    }
}
