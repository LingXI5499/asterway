package com.asterway.common.enums;

/** 执行联动的固定对象范围，只用于服务端调用，不接受任意表名或列名。 */
public enum ExecutionScopeEnum {
    TASK,
    STAGE,
    GOAL,
    WEEK,
    DAY
}
