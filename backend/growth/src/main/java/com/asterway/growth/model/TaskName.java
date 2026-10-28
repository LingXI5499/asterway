package com.asterway.growth.model;

import java.util.UUID;

// 名称查询投影不携带任务正文或其他生命周期数据。
public record TaskName(UUID taskId, String taskName) {
}
