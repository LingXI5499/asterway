/*
 * 声明方案提交回执数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.agent.mapper;

// 引入按键保存值的对应表（Map），下面可以直接使用这个类型或静态成员。
import java.util.Map;
// 引入访问数据库的 Mapper（Mapper），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Mapper;
// 引入参数（Param），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Param;

/* agent.plan_commit 幂等提交记录；跨 schema 只读写提交记录，不修改会话表。 */
// 让 MyBatis 为这个数据库访问接口创建实现。
// 定义接口 PlanCommitMapper；声明方案提交回执数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
@Mapper
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public interface PlanCommitMapper {
  // 指定 Mapper 参数在 XML 中引用时使用的名称。
  // 查找按键For更新（findByKeyForUpdate）方法：查找 按键For更新（ByKeyForUpdate）；参数负责传入本次处理所需的数据。
  // 防止重复提交的唯一键（idempotencyKey），使用 String 类型保存；可为 null，表示尚未提供。
  Map<String, Object> findByKeyForUpdate(@Param("idempotencyKey") String idempotencyKey);

  // 查找已确认方案（findConfirmedPlan）方法：查找 已确认方案（ConfirmedPlan）；参数负责传入本次处理所需的数据。
  Map<String, Object> findConfirmedPlan(
      // 指定 Mapper 参数在 XML 中引用时使用的名称。
      // 用户编号（userId），使用 long 类型保存。
      @Param("userId") long userId,
      // 指定 Mapper 参数在 XML 中引用时使用的名称。
      // 工作流运行编号（runId），使用 long 类型保存。
      @Param("runId") long runId,
      // 指定 Mapper 参数在 XML 中引用时使用的名称。
      // 方案版本号编号（planVersionId），使用 long 类型保存。
      @Param("planVersionId") long planVersionId);

  // 写入新记录（insert）方法：调用 insert 完成写入新记录处理；参数负责传入本次处理所需的数据。
  // 当前记录（row），使用 Map<String, Object> 类型保存。
  int insert(Map<String, Object> row);

  // 指定 Mapper 参数在 XML 中引用时使用的名称。
  // mark运行中（markRunning）方法：调用 markRunning 完成mark运行中处理；参数负责传入本次处理所需的数据。
  // 编号（id），使用 long 类型保存。
  // 最大尝试次数（maxAttempts），使用 int 类型保存。
  int markRunning(@Param("id") long id, @Param("maxAttempts") int maxAttempts);

  // 指定 Mapper 参数在 XML 中引用时使用的名称。
  // mark已成功（markSucceeded）方法：调用 markSucceeded 完成mark已成功处理；参数负责传入本次处理所需的数据。
  // 编号（id），使用 long 类型保存。
  // 结果JSON数据（resultJson），使用 String 类型保存；可为 null，表示尚未提供。
  int markSucceeded(@Param("id") long id, @Param("resultJson") String resultJson);

  // mark失败（markFailed）方法：调用 markFailed 完成mark失败处理；参数负责传入本次处理所需的数据。
  int markFailed(
      // 指定 Mapper 参数在 XML 中引用时使用的名称。
      // 编号（id），使用 long 类型保存。
      @Param("id") long id,
      // 指定 Mapper 参数在 XML 中引用时使用的名称。
      // 错误编码（errorCode），使用 String 类型保存；可为 null，表示尚未提供。
      @Param("errorCode") String errorCode,
      // 指定 Mapper 参数在 XML 中引用时使用的名称。
      // 错误消息（errorMessage），使用 String 类型保存；可为 null，表示尚未提供。
      @Param("errorMessage") String errorMessage);
}
