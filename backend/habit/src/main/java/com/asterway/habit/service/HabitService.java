/*
 * 规定习惯业务服务可供其他代码调用的业务方法。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.habit.service;

// 引入当前页查询条件（PageQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.PageQueryDTO;
// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入*，下面可以直接使用这个类型或静态成员。
import com.asterway.habit.dto.*;
// 引入*，下面可以直接使用这个类型或静态成员。
import com.asterway.habit.vo.*;

/* 习惯主链，规则、生命周期和从属打卡在此统一维护。 */
// 定义接口 HabitService；规定习惯业务服务可供其他代码调用的业务方法。
public interface HabitService {
    /* 分页读取本人习惯，默认不含结束归档项。 */
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    // 查询条件（query），使用 HabitQueryDTO 类型保存。
    PageResultVO<HabitVO> page(HabitQueryDTO query);

    /* 读取当前资料与当日打卡状态。 */
    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    HabitVO get(long id);

    /* 创建独立习惯及首个完整规则快照。 */
    // 创建（create）方法：调用 create 完成创建处理；参数负责传入本次处理所需的数据。
    // 请求参数对象（dto），使用 HabitSaveDTO 类型保存。
    HabitVO create(HabitSaveDTO dto);

    /* 部分修改或完整替换规则；只对当前未封存日期生效。 */
    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 HabitSaveDTO 类型保存。
    // 整体替换（replace），使用 boolean 类型保存。
    HabitVO update(long id, HabitSaveDTO dto, boolean replace);

    /* 暂停 ACTIVE 习惯，记录生命周期快照。 */
    // 暂停（pause）方法：调用 pause 完成暂停处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    HabitVO pause(long id, long version);

    /* 恢复 PAUSED 习惯，ENDED 须使用重新开启。 */
    // 继续（resume）方法：调用 resume 完成继续处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    HabitVO resume(long id, long version);

    /* 结束并归档习惯，保留已完成日和过去规则。 */
    // 结束（end）方法：调用 end 完成结束处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    HabitVO end(long id, long version);

    /* 重新开启 ENDED 习惯，不恢复逻辑删除项。 */
    // reopen方法：调用 reopen 完成reopen处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    HabitVO reopen(long id, long version);

    /* 逻辑删除习惯并追加删除快照，事实继续保留。 */
    // 删除（delete）方法：调用 delete 完成删除处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    void delete(long id, long version);

    /* 分页读取今日应做或已有打卡的习惯。 */
    // 今天（today）方法：调用 today 完成今天处理；参数负责传入本次处理所需的数据。
    // 查询条件（query），使用 PageQueryDTO 类型保存。
    PageResultVO<HabitTodayVO> today(PageQueryDTO query);

    /* 为今天打卡，版本指习惯版本；已有有效记录时幂等返回。 */
    // 打卡（checkin）方法：核对 in；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 习惯版本号（habitVersion），使用 long 类型保存。
    HabitCheckinVO checkin(long id, long habitVersion);

    /* 撤销今天的指定打卡，版本指打卡版本，不允许改写历史。 */
    // 取消（cancel）方法：调用 cancel 完成取消处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 打卡编号（checkinId），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    HabitCheckinVO cancel(long id, long checkinId, long version);

    /* 分页读取本人原始打卡和撤销事实，删除后仍可查。 */
    // 打卡记录（checkins）方法：核对 ins；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 查询条件（query），使用 HabitCheckinQueryDTO 类型保存。
    PageResultVO<HabitCheckinVO> checkins(long id, HabitCheckinQueryDTO query);

    /* 分页读取完整规则和生命周期修订，不提供修改历史接口。 */
    // 修订记录（revisions）方法：调用 revisions 完成修订记录处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    // 查询条件（query），使用 PageQueryDTO 类型保存。
    PageResultVO<HabitRevisionVO> revisions(long id, PageQueryDTO query);
}
