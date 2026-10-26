package com.asterway.app.application.identity;

import com.asterway.app.mapper.AccountExecutionMapper;
import com.asterway.identity.api.AccountExecutionPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// 执行表尚不存在时没有可持久化的执行；出现表后按真实状态检查，不能固定返回 false。
@Component
public class AccountExecutionAdapter implements AccountExecutionPort {

    private final AccountExecutionMapper mapper;

    public AccountExecutionAdapter(AccountExecutionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public int unfinishedCount(UUID userId) {
        int count = mapper.exists() ? mapper.count(userId) : 0;
        return count;
    }

    @Override
    public List<Map<String, Object>> unfinishedHints(UUID userId) {
        if (!mapper.exists()) {
            return List.of();
        }
        List<Map<String, Object>> hints = mapper.hints(userId);
        for (Map<String, Object> hint : hints) {
            hint.put("allowedActions", List.of("OPEN_EXECUTION", "END_EXECUTION"));
        }
        return hints;
    }
}
