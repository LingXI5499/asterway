/*
 * 提供成长业务权限Constants相关的共享实现或应用配置。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.constant;

/* 本阶段已实现的成长业务权限白名单，普通角色不能获得任意未知权限。 */
// 定义类 GrowthPermissionConstants；提供成长业务权限Constants相关的共享实现或应用配置。
public final class GrowthPermissionConstants {
  // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
  private GrowthPermissionConstants() {}

  // 准备CODES，类型为 java.util.Set<String>；初始内容是调用 java.util.Set.of 完成of处理。
  public static final java.util.Set<String> CODES =
      // 继续传入或组合调用 java.util.Set.of 完成of处理，完成上面尚未结束的调用或声明。
      java.util.Set.of(
          // 继续传入或组合固定文字 'goal:goal:list'，完成上面尚未结束的调用或声明。
          "goal:goal:list",
          // 继续传入或组合固定文字 'goal:goal:read'，完成上面尚未结束的调用或声明。
          "goal:goal:read",
          // 继续传入或组合固定文字 'goal:goal:create'，完成上面尚未结束的调用或声明。
          "goal:goal:create",
          // 继续传入或组合固定文字 'goal:goal:update'，完成上面尚未结束的调用或声明。
          "goal:goal:update",
          // 继续传入或组合固定文字 'goal:goal:status'，完成上面尚未结束的调用或声明。
          "goal:goal:status",
          // 继续传入或组合固定文字 'goal:goal:archive'，完成上面尚未结束的调用或声明。
          "goal:goal:archive",
          // 继续传入或组合固定文字 'goal:goal:restore'，完成上面尚未结束的调用或声明。
          "goal:goal:restore",
          // 继续传入或组合固定文字 'stage:stage:list'，完成上面尚未结束的调用或声明。
          "stage:stage:list",
          // 继续传入或组合固定文字 'stage:stage:read'，完成上面尚未结束的调用或声明。
          "stage:stage:read",
          // 继续传入或组合固定文字 'stage:stage:create'，完成上面尚未结束的调用或声明。
          "stage:stage:create",
          // 继续传入或组合固定文字 'stage:stage:update'，完成上面尚未结束的调用或声明。
          "stage:stage:update",
          // 继续传入或组合固定文字 'stage:stage:status'，完成上面尚未结束的调用或声明。
          "stage:stage:status",
          // 继续传入或组合固定文字 'stage:stage:delete'，完成上面尚未结束的调用或声明。
          "stage:stage:delete",
          // 继续传入或组合固定文字 'stage:stage:sort'，完成上面尚未结束的调用或声明。
          "stage:stage:sort",
          // 继续传入或组合固定文字 'task:task:list'，完成上面尚未结束的调用或声明。
          "task:task:list",
          // 继续传入或组合固定文字 'task:task:read'，完成上面尚未结束的调用或声明。
          "task:task:read",
          // 继续传入或组合固定文字 'task:task:create'，完成上面尚未结束的调用或声明。
          "task:task:create",
          // 继续传入或组合固定文字 'task:task:update'，完成上面尚未结束的调用或声明。
          "task:task:update",
          // 继续传入或组合固定文字 'task:task:status'，完成上面尚未结束的调用或声明。
          "task:task:status",
          // 继续传入或组合固定文字 'task:task:delete'，完成上面尚未结束的调用或声明。
          "task:task:delete",
          // 继续传入或组合固定文字 'task:task:sort'，完成上面尚未结束的调用或声明。
          "task:task:sort",
          // 继续传入或组合固定文字 'planning:weekly:list'，完成上面尚未结束的调用或声明。
          "planning:weekly:list",
          // 继续传入或组合固定文字 'planning:weekly:read'，完成上面尚未结束的调用或声明。
          "planning:weekly:read",
          // 继续传入或组合固定文字 'planning:weekly:manage'，完成上面尚未结束的调用或声明。
          "planning:weekly:manage",
          // 继续传入或组合固定文字 'planning:daily:list'，完成上面尚未结束的调用或声明。
          "planning:daily:list",
          // 继续传入或组合固定文字 'planning:daily:read'，完成上面尚未结束的调用或声明。
          "planning:daily:read",
          // 继续传入或组合固定文字 'planning:daily:manage'，完成上面尚未结束的调用或声明。
          "planning:daily:manage",
          // 继续传入或组合固定文字 'planning:assignment:read'，完成上面尚未结束的调用或声明。
          "planning:assignment:read",
          // 继续传入或组合固定文字 'planning:assignment:manage'，完成上面尚未结束的调用或声明。
          "planning:assignment:manage",
          // 继续传入或组合固定文字 'planning:assignment:remove'，完成上面尚未结束的调用或声明。
          "planning:assignment:remove",
          // 继续传入或组合固定文字 'planning:pending:list'，完成上面尚未结束的调用或声明。
          "planning:pending:list",
          // 继续传入或组合固定文字 'execution:session:list'，完成上面尚未结束的调用或声明。
          "execution:session:list",
          // 继续传入或组合固定文字 'execution:session:read'，完成上面尚未结束的调用或声明。
          "execution:session:read",
          // 继续传入或组合固定文字 'execution:session:start'，完成上面尚未结束的调用或声明。
          "execution:session:start",
          // 继续传入或组合固定文字 'execution:session:pause'，完成上面尚未结束的调用或声明。
          "execution:session:pause",
          // 继续传入或组合固定文字 'execution:session:resume'，完成上面尚未结束的调用或声明。
          "execution:session:resume",
          // 继续传入或组合固定文字 'execution:session:finish'，完成上面尚未结束的调用或声明。
          "execution:session:finish",
          // 继续传入或组合固定文字 'execution:session:heartbeat'，完成上面尚未结束的调用或声明。
          "execution:session:heartbeat",
          // 继续传入或组合固定文字 'habit:category:list'，完成上面尚未结束的调用或声明。
          "habit:category:list",
          // 继续传入或组合固定文字 'habit:category:create'，完成上面尚未结束的调用或声明。
          "habit:category:create",
          // 继续传入或组合固定文字 'habit:category:update'，完成上面尚未结束的调用或声明。
          "habit:category:update",
          // 继续传入或组合固定文字 'habit:category:delete'，完成上面尚未结束的调用或声明。
          "habit:category:delete",
          // 继续传入或组合固定文字 'habit:habit:list'，完成上面尚未结束的调用或声明。
          "habit:habit:list",
          // 继续传入或组合固定文字 'habit:habit:read'，完成上面尚未结束的调用或声明。
          "habit:habit:read",
          // 继续传入或组合固定文字 'habit:habit:create'，完成上面尚未结束的调用或声明。
          "habit:habit:create",
          // 继续传入或组合固定文字 'habit:habit:update'，完成上面尚未结束的调用或声明。
          "habit:habit:update",
          // 继续传入或组合固定文字 'habit:habit:pause'，完成上面尚未结束的调用或声明。
          "habit:habit:pause",
          // 继续传入或组合固定文字 'habit:habit:resume'，完成上面尚未结束的调用或声明。
          "habit:habit:resume",
          // 继续传入或组合固定文字 'habit:habit:end'，完成上面尚未结束的调用或声明。
          "habit:habit:end",
          // 继续传入或组合固定文字 'habit:habit:reopen'，完成上面尚未结束的调用或声明。
          "habit:habit:reopen",
          // 继续传入或组合固定文字 'habit:habit:delete'，完成上面尚未结束的调用或声明。
          "habit:habit:delete",
          // 继续传入或组合固定文字 'habit:habit:history'，完成上面尚未结束的调用或声明。
          "habit:habit:history",
          // 继续传入或组合固定文字 'habit:checkin:create'，完成上面尚未结束的调用或声明。
          "habit:checkin:create",
          // 继续传入或组合固定文字 'habit:checkin:cancel'，完成上面尚未结束的调用或声明。
          "habit:checkin:cancel",
          // 继续传入或组合固定文字 'habit:checkin:read'，完成上面尚未结束的调用或声明。
          "habit:checkin:read",
          // 继续传入或组合固定文字 'notification:message:list'，完成上面尚未结束的调用或声明。
          "notification:message:list",
          // 继续传入或组合固定文字 'notification:message:read'，完成上面尚未结束的调用或声明。
          "notification:message:read",
          // 继续传入或组合固定文字 'notification:message:mark_read'，完成上面尚未结束的调用或声明。
          "notification:message:mark_read",
          // 继续传入或组合固定文字 'notification:message:handle'，完成上面尚未结束的调用或声明。
          "notification:message:handle",
          // 继续传入或组合固定文字 'dashboard:board:read'，完成上面尚未结束的调用或声明。
          "dashboard:board:read",
          // 继续传入或组合固定文字 'statistics:report:read'，完成上面尚未结束的调用或声明。
          "statistics:report:read",
          // 继续传入或组合固定文字 'agent:chat:read'，完成上面尚未结束的调用或声明。
          "agent:chat:read",
          // 继续传入或组合固定文字 'agent:chat:send'，完成上面尚未结束的调用或声明。
          "agent:chat:send",
          // 继续传入或组合固定文字 'agent:conversation:list'，完成上面尚未结束的调用或声明。
          "agent:conversation:list",
          // 继续传入或组合固定文字 'agent:conversation:read'，完成上面尚未结束的调用或声明。
          "agent:conversation:read",
          // 继续传入或组合固定文字 'agent:conversation:create'，完成上面尚未结束的调用或声明。
          "agent:conversation:create",
          // 继续传入或组合固定文字 'agent:conversation:update'，完成上面尚未结束的调用或声明。
          "agent:conversation:update",
          // 继续传入或组合固定文字 'agent:conversation:close'，完成上面尚未结束的调用或声明。
          "agent:conversation:close",
          // 继续传入或组合固定文字 'agent:conversation:delete'，完成上面尚未结束的调用或声明。
          "agent:conversation:delete",
          // 继续传入或组合固定文字 'agent:memory:read'，完成上面尚未结束的调用或声明。
          "agent:memory:read",
          // 继续传入或组合固定文字 'agent:memory:manage'，完成上面尚未结束的调用或声明。
          "agent:memory:manage",
          // 继续传入或组合固定文字 'agent:knowledge:read'，完成上面尚未结束的调用或声明。
          "agent:knowledge:read",
          // 继续传入或组合固定文字 'agent:knowledge:manage'，完成上面尚未结束的调用或声明。
          "agent:knowledge:manage",
          // 继续传入或组合固定文字 'agent:workflow:read'，完成上面尚未结束的调用或声明。
          "agent:workflow:read",
          // 继续传入或组合固定文字 'agent:workflow:manage'，完成上面尚未结束的调用或声明。
          "agent:workflow:manage",
          // 继续传入或组合固定文字 'agent:review:read'，完成上面尚未结束的调用或声明。
          "agent:review:read",
          // 继续传入或组合固定文字 'agent:review:manage'，完成上面尚未结束的调用或声明。
          "agent:review:manage",
          // 继续传入或组合表达式 "agent:run:read") 的结果，完成上面尚未结束的调用或声明。
          "agent:run:read");
}
