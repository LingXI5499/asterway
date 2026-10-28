package com.asterway.growth.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// 仅提供本人任务名称，其他模块不能直接查询成长模块的表。
public interface TaskQueryApi {

    Map<UUID, String> taskNames(UUID userId, List<UUID> taskIds);
}
