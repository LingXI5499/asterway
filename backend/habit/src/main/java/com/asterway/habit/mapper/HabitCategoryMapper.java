/*
 * 声明习惯分类数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.habit.mapper;

// 引入习惯查询条件（HabitQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.dto.HabitQueryDTO;
// 引入习惯分类（HabitCategoryEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.entity.HabitCategoryEntity;

// 引入访问数据库的 Mapper（Mapper），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Mapper;
// 引入参数（Param），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Param;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;

/* habit 持久化接口；所有 SQL 位于对应 XML。 */
// 让 MyBatis 为这个数据库访问接口创建实现。
// 定义接口 HabitCategoryMapper；声明习惯分类数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
@Mapper
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public interface HabitCategoryMapper {
    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 查找（find）方法：取得第一条符合条件的数据；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    HabitCategoryEntity find(@Param("userId") long userId, @Param("id") long id);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 查询条件（query），使用 HabitQueryDTO 类型保存。
    List<HabitCategoryEntity> page(@Param("userId") long userId, @Param("q") HabitQueryDTO query);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 数量（count）方法：调用 count 完成数量处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 查询条件（query），使用 HabitQueryDTO 类型保存。
    long count(@Param("userId") long userId, @Param("q") HabitQueryDTO query);

    // 名称是否存在（nameExists）方法：调用 nameExists 完成名称是否存在处理；参数负责传入本次处理所需的数据。
    boolean nameExists(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 用户编号（userId），使用 long 类型保存。
            @Param("userId") long userId,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 名称（name），使用 String 类型保存；可为 null，表示尚未提供。
            @Param("name") String name,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // except编号（exceptId），使用 Long 类型保存；可为 null，表示尚未提供。
            @Param("exceptId") Long exceptId);

    // 写入新记录（insert）方法：调用 insert 完成写入新记录处理；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 HabitCategoryEntity 类型保存。
    int insert(HabitCategoryEntity row);

    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 HabitCategoryEntity 类型保存。
    int update(HabitCategoryEntity row);

    // soft删除（softDelete）方法：调用 softDelete 完成soft删除处理；参数负责传入本次处理所需的数据。
    int softDelete(
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
}
