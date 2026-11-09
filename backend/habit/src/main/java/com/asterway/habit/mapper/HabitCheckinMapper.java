/*
 * 声明习惯打卡数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.habit.mapper;

// 引入习惯打卡查询条件（HabitCheckinQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.dto.HabitCheckinQueryDTO;
// 引入习惯打卡（HabitCheckinEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.entity.HabitCheckinEntity;
// 引入习惯打卡（HabitCheckinVO），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.vo.HabitCheckinVO;

// 引入访问数据库的 Mapper（Mapper），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Mapper;
// 引入参数（Param），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Param;

// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;

/* habit 持久化接口；所有 SQL 位于对应 XML。 */
// 让 MyBatis 为这个数据库访问接口创建实现。
// 定义接口 HabitCheckinMapper；声明习惯打卡数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
@Mapper
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public interface HabitCheckinMapper {
    // 有效（valid）方法：调用 valid 完成有效处理；参数负责传入本次处理所需的数据。
    HabitCheckinEntity valid(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 用户编号（userId），使用 long 类型保存。
            @Param("userId") long userId,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 习惯编号（habitId），使用 long 类型保存。
            @Param("habitId") long habitId,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 今天（today），使用 LocalDate 类型保存；可为 null，表示尚未提供。
            @Param("today") LocalDate today);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 查找（find）方法：取得第一条符合条件的数据；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    HabitCheckinEntity find(@Param("userId") long userId, @Param("id") long id);

    // 写入新记录（insert）方法：调用 insert 完成写入新记录处理；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 HabitCheckinEntity 类型保存。
    int insert(HabitCheckinEntity row);

    // 取消（cancel）方法：调用 cancel 完成取消处理；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 HabitCheckinEntity 类型保存。
    int cancel(HabitCheckinEntity row);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 页面（view）方法：调用 view 完成页面处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    HabitCheckinVO view(@Param("userId") long userId, @Param("id") long id);

    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    List<HabitCheckinVO> page(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 用户编号（userId），使用 long 类型保存。
            @Param("userId") long userId,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 习惯编号（habitId），使用 long 类型保存。
            @Param("habitId") long habitId,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 查询条件（query），使用 HabitCheckinQueryDTO 类型保存。
            @Param("q") HabitCheckinQueryDTO query);

    // 数量（count）方法：调用 count 完成数量处理；参数负责传入本次处理所需的数据。
    long count(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 用户编号（userId），使用 long 类型保存。
            @Param("userId") long userId,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 习惯编号（habitId），使用 long 类型保存。
            @Param("habitId") long habitId,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 查询条件（query），使用 HabitCheckinQueryDTO 类型保存。
            @Param("q") HabitCheckinQueryDTO query);
}
