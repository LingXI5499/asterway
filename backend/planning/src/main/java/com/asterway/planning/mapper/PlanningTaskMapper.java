/*
 * 声明规划任务数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.planning.mapper;

// 引入成长业务查询条件（GrowthQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.GrowthQueryDTO;
// 引入规划任务（PlanningTaskVO），下面可以直接使用这个类型或静态成员。
import com.asterway.planning.vo.PlanningTaskVO;

// 引入访问数据库的 Mapper（Mapper），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Mapper;
// 引入参数（Param），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Param;

// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;

/* 仅负责计划任务及待处理的跨表查询，不承担单表写操作。 */
// 让 MyBatis 为这个数据库访问接口创建实现。
// 定义接口 PlanningTaskMapper；声明规划任务数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
@Mapper
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public interface PlanningTaskMapper {
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    List<PlanningTaskVO> page(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 用户编号（userId），使用 long 类型保存。
            @Param("userId") long userId,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 查询条件（query），使用 GrowthQueryDTO 类型保存。
            @Param("q") GrowthQueryDTO query,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 等待处理（pending），使用 boolean 类型保存。
            @Param("pending") boolean pending);

    // 数量（count）方法：调用 count 完成数量处理；参数负责传入本次处理所需的数据。
    long count(
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 用户编号（userId），使用 long 类型保存。
            @Param("userId") long userId,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 查询条件（query），使用 GrowthQueryDTO 类型保存。
            @Param("q") GrowthQueryDTO query,
            // 指定 Mapper 参数在 XML 中引用时使用的名称。
            // 等待处理（pending），使用 boolean 类型保存。
            @Param("pending") boolean pending);
}
