/*
 * 声明通知消息数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.notification.mapper;

// 引入通知查询条件（NotificationQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.notification.dto.NotificationQueryDTO;
// 引入通知消息（NotificationMessageEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.notification.entity.NotificationMessageEntity;
// 引入目标Condition（GoalConditionVO），下面可以直接使用这个类型或静态成员。
import com.asterway.notification.vo.GoalConditionVO;
// 引入通知消息（NotificationMessageVO），下面可以直接使用这个类型或静态成员。
import com.asterway.notification.vo.NotificationMessageVO;

// 引入访问数据库的 Mapper（Mapper），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Mapper;
// 引入参数（Param），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Param;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;

/* 消息与目标条件在相同数据库事务内维护，不引入外部发件箱。 */
// 让 MyBatis 为这个数据库访问接口创建实现。
// 定义接口 NotificationMessageMapper；声明通知消息数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
@Mapper
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public interface NotificationMessageMapper {
    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 查找（find）方法：取得第一条符合条件的数据；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    NotificationMessageEntity find(@Param("userId") long userId, @Param("id") long id);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 页面（view）方法：调用 view 完成页面处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    NotificationMessageVO view(@Param("userId") long userId, @Param("id") long id);

    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    List<NotificationMessageVO> page(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 用户编号（userId），使用 long 类型保存。
            // 查询条件（query），使用 NotificationQueryDTO 类型保存。
            @Param("userId") long userId, @Param("q") NotificationQueryDTO query);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 数量（count）方法：调用 count 完成数量处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 查询条件（query），使用 NotificationQueryDTO 类型保存。
    long count(@Param("userId") long userId, @Param("q") NotificationQueryDTO query);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // unread方法：调用 unread 完成unread处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    long unread(@Param("userId") long userId);

    // 写入新记录（insert）方法：调用 insert 完成写入新记录处理；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 NotificationMessageEntity 类型保存。
    int insert(NotificationMessageEntity row);

    // mark读取（markRead）方法：调用 markRead 完成mark读取处理；参数负责传入本次处理所需的数据。
    int markRead(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 用户编号（userId），使用 long 类型保存。
            @Param("userId") long userId,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 编号（id），使用 long 类型保存。
            @Param("id") long id,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 版本号（version），使用 long 类型保存。
            @Param("version") long version,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 当前时间（now），使用 Instant 类型保存；可为 null，表示尚未提供。
            @Param("now") Instant now);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // mark全部读取（markAllRead）方法：调用 markAllRead 完成mark全部读取处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 当前时间（now），使用 Instant 类型保存；可为 null，表示尚未提供。
    int markAllRead(@Param("userId") long userId, @Param("now") Instant now);

    // handle方法：调用 handle 完成handle处理；参数负责传入本次处理所需的数据。
    int handle(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 用户编号（userId），使用 long 类型保存。
            @Param("userId") long userId,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 编号（id），使用 long 类型保存。
            @Param("id") long id,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 版本号（version），使用 long 类型保存。
            @Param("version") long version,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 状态（status），使用 String 类型保存；可为 null，表示尚未提供。
            @Param("status") String status,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 当前时间（now），使用 Instant 类型保存；可为 null，表示尚未提供。
            @Param("now") Instant now);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // condition方法：调用 condition 完成condition处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 目标编号（goalId），使用 long 类型保存。
    GoalConditionVO condition(@Param("userId") long userId, @Param("goalId") long goalId);

    // 更新Condition（updateCondition）方法：更新 Condition；参数负责传入本次处理所需的数据。
    int updateCondition(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 用户编号（userId），使用 long 类型保存。
            @Param("userId") long userId,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 目标编号（goalId），使用 long 类型保存。
            @Param("goalId") long goalId,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 版本号（version），使用 long 类型保存。
            @Param("version") long version,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // met，使用 boolean 类型保存。
            @Param("met") boolean met,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 轮（round），使用 long 类型保存。
            @Param("round") long round,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 当前时间（now），使用 Instant 类型保存；可为 null，表示尚未提供。
            @Param("now") Instant now);

    // invalidate目标（invalidateGoal）方法：调用 invalidateGoal 完成invalidate目标处理；参数负责传入本次处理所需的数据。
    int invalidateGoal(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 用户编号（userId），使用 long 类型保存。
            @Param("userId") long userId,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 目标编号（goalId），使用 long 类型保存。
            @Param("goalId") long goalId,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // keep轮（keepRound），使用 long 类型保存。
            @Param("keepRound") long keepRound,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 当前时间（now），使用 Instant 类型保存；可为 null，表示尚未提供。
            @Param("now") Instant now);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 清除用户Conditions（clearUserConditions）方法：调用 clearUserConditions 完成清除用户Conditions处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 当前时间（now），使用 Instant 类型保存；可为 null，表示尚未提供。
    int clearUserConditions(@Param("userId") long userId, @Param("now") Instant now);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // invalidate用户（invalidateUser）方法：调用 invalidateUser 完成invalidate用户处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 当前时间（now），使用 Instant 类型保存；可为 null，表示尚未提供。
    int invalidateUser(@Param("userId") long userId, @Param("now") Instant now);
}
