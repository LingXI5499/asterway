/*
 * 提供执行计时Constants相关的共享实现或应用配置。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.constant;

// 引入设置（Set），下面可以直接使用这个类型或静态成员。
import java.util.Set;

/* 执行计时的状态、停止原因和动作权限，不混入其他模块的常量。 */
// 定义类 ExecutionConstants；提供执行计时Constants相关的共享实现或应用配置。
public final class ExecutionConstants {
    // 准备运行中（RUNNING），类型为 String；初始内容是固定状态或编码 RUNNING（运行中）。
    public static final String RUNNING = "RUNNING";
    // 准备已暂停（PAUSED），类型为 String；初始内容是固定状态或编码 PAUSED（已暂停）。
    public static final String PAUSED = "PAUSED";
    // 准备FINISHED，类型为 String；初始内容是固定状态或编码 FINISHED（FINISHED）。
    public static final String FINISHED = "FINISHED";
    // 准备用户暂停（USER_PAUSE），类型为 String；初始内容是固定状态或编码 USER_PAUSE（用户暂停）。
    public static final String USER_PAUSE = "USER_PAUSE";
    // 准备AUTO暂停（AUTO_PAUSE），类型为 String；初始内容是固定状态或编码 AUTO_PAUSE（AUTO暂停）。
    public static final String AUTO_PAUSE = "AUTO_PAUSE";
    // 准备方案BOUNDARY（PLAN_BOUNDARY），类型为 String；初始内容是固定状态或编码 PLAN_BOUNDARY（方案BOUNDARY）。
    public static final String PLAN_BOUNDARY = "PLAN_BOUNDARY";
    // 准备认证REVOKED（AUTH_REVOKED），类型为 String；初始内容是固定状态或编码 AUTH_REVOKED（认证REVOKED）。
    public static final String AUTH_REVOKED = "AUTH_REVOKED";
    // 准备FINISH，类型为 String；初始内容是固定状态或编码 FINISH（FINISH）。
    public static final String FINISH = "FINISH";
    // 准备用户FINISH（USER_FINISH），类型为 String；初始内容是固定状态或编码 USER_FINISH（用户FINISH）。
    public static final String USER_FINISH = "USER_FINISH";
    // 准备任务删除标记（TASK_DELETED），类型为 String；初始内容是固定状态或编码 TASK_DELETED（任务删除标记）。
    public static final String TASK_DELETED = "TASK_DELETED";
    // 准备目标已归档（GOAL_ARCHIVED），类型为 String；初始内容是固定状态或编码 GOAL_ARCHIVED（目标已归档）。
    public static final String GOAL_ARCHIVED = "GOAL_ARCHIVED";
    // 准备列表（LIST），类型为 String；初始内容是固定文字 'execution:session:list'。
    public static final String LIST = "execution:session:list";
    // 准备读取（READ），类型为 String；初始内容是固定文字 'execution:session:read'。
    public static final String READ = "execution:session:read";
    // 准备开始（START），类型为 String；初始内容是固定文字 'execution:session:start'。
    public static final String START = "execution:session:start";
    // 准备暂停（PAUSE），类型为 String；初始内容是固定文字 'execution:session:pause'。
    public static final String PAUSE = "execution:session:pause";
    // 准备继续（RESUME），类型为 String；初始内容是固定文字 'execution:session:resume'。
    public static final String RESUME = "execution:session:resume";
    // 准备结束（END），类型为 String；初始内容是固定文字 'execution:session:finish'。
    public static final String END = "execution:session:finish";
    // 准备心跳（HEARTBEAT），类型为 String；初始内容是固定文字 'execution:session:heartbeat'。
    public static final String HEARTBEAT = "execution:session:heartbeat";
    // 准备权限列表（PERMISSIONS），类型为 Set<String>；初始内容是调用 Set.of 完成of处理。
    public static final Set<String> PERMISSIONS =
            // 调用 Set.of 完成of处理。
            Set.of(LIST, READ, START, PAUSE, RESUME, END, HEARTBEAT);
    // 准备STATES，类型为 Set<String>；初始内容是调用 Set.of 完成of处理。
    public static final Set<String> STATES = Set.of(RUNNING, PAUSED, FINISHED);

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    private ExecutionConstants() {}
}
