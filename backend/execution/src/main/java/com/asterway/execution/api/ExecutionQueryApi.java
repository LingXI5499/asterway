package com.asterway.execution.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// 只读执行状态，账户用例的跨模块组合由 app 完成。
public interface ExecutionQueryApi {

    int unfinishedCount(UUID userId);

    List<Map<String, Object>> unfinishedHints(UUID userId);
}
