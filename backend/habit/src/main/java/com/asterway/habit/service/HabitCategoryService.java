/*
 * 规定习惯分类业务服务可供其他代码调用的业务方法。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.habit.service;

// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入习惯分类保存（HabitCategorySaveDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.dto.HabitCategorySaveDTO;
// 引入习惯查询条件（HabitQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.dto.HabitQueryDTO;
// 引入习惯分类（HabitCategoryVO），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.vo.HabitCategoryVO;

/* 用户自有分类的一条完整业务链路。 */
// 定义接口 HabitCategoryService；规定习惯分类业务服务可供其他代码调用的业务方法。
public interface HabitCategoryService {
    /* 分页读取本人仍有效的分类。 */
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    // 查询条件（query），使用 HabitQueryDTO 类型保存。
    PageResultVO<HabitCategoryVO> page(HabitQueryDTO query);

    /* 创建本人分类，同用户有效名称不重复。 */
    // 创建（create）方法：调用 create 完成创建处理；参数负责传入本次处理所需的数据。
    // 请求参数对象（dto），使用 HabitCategorySaveDTO 类型保存。
    HabitCategoryVO create(HabitCategorySaveDTO dto);

    /* 按版本修改分类名称和顺序；无变化不推进版本。 */
    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 HabitCategorySaveDTO 类型保存。
    HabitCategoryVO update(long id, HabitCategorySaveDTO dto);

    /* 逻辑删除分类，保留既有习惯和历史引用。 */
    // 删除（delete）方法：调用 delete 完成删除处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    void delete(long id, long version);
}
