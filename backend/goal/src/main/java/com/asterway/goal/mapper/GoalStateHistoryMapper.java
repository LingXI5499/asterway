/*
 * 声明目标当前状态历史数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.goal.mapper;

// 引入目标当前状态历史（GoalStateHistoryEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.goal.entity.GoalStateHistoryEntity;
// 引入目标当前状态历史（GoalStateHistoryVO），下面可以直接使用这个类型或静态成员。
import com.asterway.goal.vo.GoalStateHistoryVO;

// 引入访问数据库的 Mapper（Mapper），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Mapper;
// 引入参数（Param），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Param;

// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;

/* 主体从属历史，只允许追加和本人范围读取。 */
// 让 MyBatis 为这个数据库访问接口创建实现。
// 定义接口 GoalStateHistoryMapper；声明目标当前状态历史数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
@Mapper
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public interface GoalStateHistoryMapper {
    // 写入新记录（insert）方法：调用 insert 完成写入新记录处理；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 GoalStateHistoryEntity 类型保存。
    int insert(GoalStateHistoryEntity row);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // historicalSubject方法：调用 historicalSubject 完成historicalSubject处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    Long historicalSubject(@Param("userId") long userId, @Param("id") long id);

    // 指定 Mapper 参数在 XML 中引用时使用的名称。
    // 数量（count）方法：调用 count 完成数量处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    long count(@Param("userId") long userId, @Param("id") long id);

    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    List<GoalStateHistoryVO> page(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 用户编号（userId），使用 long 类型保存。
            @Param("userId") long userId,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 编号（id），使用 long 类型保存。
            @Param("id") long id,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 分页时跳过的记录数（offset），使用 int 类型保存。
            @Param("offset") int offset,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 每页条数（size），使用 int 类型保存。
            @Param("size") int size);
}
