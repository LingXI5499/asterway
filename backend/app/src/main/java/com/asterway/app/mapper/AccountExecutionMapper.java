package com.asterway.app.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// 账户用例的只读跨模块适配，不实现执行计时或结束业务。
public interface AccountExecutionMapper {

    boolean exists();

    int count(@Param("userId") UUID userId);

    List<Map<String, Object>> hints(@Param("userId") UUID userId);
}
