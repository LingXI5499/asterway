/*
 * 声明执行计时详情返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.execution.vo;

// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;

/* 会话详情及首屏时间段，可使用专属分页接口继续读取。 */
// 定义保存一组固定字段的 record ExecutionDetailVO；声明执行计时详情返回给页面的字段，不直接把数据库实体全部暴露出去。
public record ExecutionDetailVO(
        // 执行计时（execution），使用 ExecutionSessionVO 类型保存。
        // 计时段列表（segments），使用 PageResultVO<ExecutionSegmentVO> 类型保存。
        ExecutionSessionVO execution, PageResultVO<ExecutionSegmentVO> segments) {}
