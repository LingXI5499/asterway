package com.asterway.identity.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// app 只读适配既有执行状态；identity 不读取兄弟模块的数据表。
public interface AccountExecutionPort {

    int unfinishedCount(UUID userId);

    List<Map<String, Object>> unfinishedHints(UUID userId);
}
