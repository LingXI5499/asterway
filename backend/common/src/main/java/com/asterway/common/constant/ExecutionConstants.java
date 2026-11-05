package com.asterway.common.constant;

import java.util.Set;

/** 执行计时的状态、停止原因和动作权限，不混入其他模块的常量。 */
public final class ExecutionConstants {
    public static final String RUNNING = "RUNNING";
    public static final String PAUSED = "PAUSED";
    public static final String FINISHED = "FINISHED";
    public static final String USER_PAUSE = "USER_PAUSE";
    public static final String AUTO_PAUSE = "AUTO_PAUSE";
    public static final String PLAN_BOUNDARY = "PLAN_BOUNDARY";
    public static final String AUTH_REVOKED = "AUTH_REVOKED";
    public static final String FINISH = "FINISH";
    public static final String USER_FINISH = "USER_FINISH";
    public static final String TASK_DELETED = "TASK_DELETED";
    public static final String GOAL_ARCHIVED = "GOAL_ARCHIVED";
    public static final String LIST = "execution:session:list";
    public static final String READ = "execution:session:read";
    public static final String START = "execution:session:start";
    public static final String PAUSE = "execution:session:pause";
    public static final String RESUME = "execution:session:resume";
    public static final String END = "execution:session:finish";
    public static final String HEARTBEAT = "execution:session:heartbeat";
    public static final Set<String> PERMISSIONS =
            Set.of(LIST, READ, START, PAUSE, RESUME, END, HEARTBEAT);
    public static final Set<String> STATES = Set.of(RUNNING, PAUSED, FINISHED);

    private ExecutionConstants() {}
}
