/*
 * 规定目标业务服务可供其他代码调用的业务方法。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.goal.service;

// 引入成长业务查询条件（GrowthQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.GrowthQueryDTO;
// 引入成长业务状态（GrowthStatusDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.GrowthStatusDTO;
// 引入当前页查询条件（PageQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.PageQueryDTO;
// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入目标归档（GoalArchiveDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.goal.dto.GoalArchiveDTO;
// 引入目标保存（GoalSaveDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.goal.dto.GoalSaveDTO;
// 引入目标当前状态历史（GoalStateHistoryVO），下面可以直接使用这个类型或静态成员。
import com.asterway.goal.vo.GoalStateHistoryVO;
// 引入目标（GoalVO），下面可以直接使用这个类型或静态成员。
import com.asterway.goal.vo.GoalVO;

/* Goal 的完整业务链路，不按增删改查拆分。 */
// 定义接口 GoalService；规定目标业务服务可供其他代码调用的业务方法。
public interface GoalService {
    /* 本人状态事实分页，归档或逻辑删除后仍保留历史读取。 */
    // 历史（history）方法：调用 history 完成历史处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 查询条件（query），使用 PageQueryDTO 类型保存。
    PageResultVO<GoalStateHistoryVO> history(long id, PageQueryDTO query);

    /* 当前用户范围内的业务操作。 */
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    // 查询条件（query），使用 GrowthQueryDTO 类型保存。
    PageResultVO<GoalVO> page(GrowthQueryDTO query);

    /* 当前用户范围内的业务操作。 */
    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    GoalVO get(long id);

    /* 当前用户范围内的业务操作。 */
    // 创建（create）方法：调用 create 完成创建处理；参数负责传入本次处理所需的数据。
    // 请求参数对象（dto），使用 GoalSaveDTO 类型保存。
    GoalVO create(GoalSaveDTO dto);

    /* 当前用户范围内的业务操作。 */
    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 GoalSaveDTO 类型保存。
    GoalVO update(long id, GoalSaveDTO dto);

    /* 当前用户范围内的业务操作。 */
    /* 完整替换资料，可清空可选日期。 */
    // 整体替换（replace）方法：生成保留原对象其余字段的新副本，只替换明确指定的值；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 GoalSaveDTO 类型保存。
    GoalVO replace(long id, GoalSaveDTO dto);

    // 状态（status）方法：调用 status 完成状态处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 GrowthStatusDTO 类型保存。
    GoalVO status(long id, GrowthStatusDTO dto);

    /* 当前用户范围内的业务操作。 */
    // 删除（delete）方法：调用 delete 完成删除处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    void delete(long id, long version);

    /* 当前用户范围内的业务操作。 */
    // 归档（archive）方法：调用 archive 完成归档处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 GoalArchiveDTO 类型保存。
    GoalVO archive(long id, GoalArchiveDTO dto);

    /* 当前用户范围内的业务操作。 */
    // 恢复（restore）方法：调用 restore 完成恢复处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    GoalVO restore(long id, long version);
}
