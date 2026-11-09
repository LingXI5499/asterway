/*
 * 实现习惯方案写入业务服务业务规则，处理权限、状态、数据读写和必要的事务。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.habit.service.impl;

// 引入方案写入Port（PlanWritePort），下面可以直接使用这个类型或静态成员。
import com.asterway.common.integration.PlanWritePort;
// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入方案写入提交数据Utils（PlanWritePayloadUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.PlanWritePayloadUtils;
// 引入习惯保存（HabitSaveDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.dto.HabitSaveDTO;
// 引入习惯查询条件（HabitQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.dto.HabitQueryDTO;
// 引入习惯分类保存（HabitCategorySaveDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.dto.HabitCategorySaveDTO;
// 引入习惯业务服务（HabitService），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.service.HabitService;
// 引入习惯分类业务服务（HabitCategoryService），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.service.HabitCategoryService;
// 引入JSON数据节点（JsonNode），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.databind.JsonNode;
// 引入JSON 与 Java 对象转换器（ObjectMapper），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.databind.ObjectMapper;
// 引入业务服务（Service），下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Service;

/* 习惯独立创建；分类按当前用户的真实数据复用。 */
// 把该实现交给 Spring 管理，其他组件可以注入并调用。
// 定义类 HabitPlanWriteServiceImpl；实现习惯方案写入业务服务业务规则，处理权限、状态、数据读写和必要的事务。
@Service
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class HabitPlanWriteServiceImpl implements PlanWritePort {
    // 习惯列表（habits），使用 HabitService 类型保存。
    private final HabitService habits;
    // 分类列表（categories），使用 HabitCategoryService 类型保存。
    private final HabitCategoryService categories;
    // JSON 编码和解析库（json），使用 ObjectMapper 类型保存。
    private final ObjectMapper json;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 习惯列表（habits），使用 HabitService 类型保存。
    // 分类列表（categories），使用 HabitCategoryService 类型保存。
    // JSON 编码和解析库（json），使用 ObjectMapper 类型保存。
    public HabitPlanWriteServiceImpl(HabitService habits, HabitCategoryService categories, ObjectMapper json) {
        // 把习惯列表（habits）保存到 this.habits。
        this.habits = habits;
        // 把分类列表（categories）保存到 this.categories。
        this.categories = categories;
        // 把JSON 编码和解析库（json）保存到 this.json。
        this.json = json;
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // domain方法：调用 domain 完成domain处理；参数负责传入本次处理所需的数据。
    @Override
    // 把固定文字 'habit'交还给调用方，本方法到此结束。
    public String domain() { return "habit"; }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 写入（write）方法：把内容写入当前输出或响应；参数负责传入本次处理所需的数据。
    @Override
    // 操作（action），使用 Action 类型保存。
    // command，使用 JsonNode 类型保存。
    public JsonNode write(Action action, JsonNode command) {
        // 检查条件 action == Action.CREATE 是否成立；成立时执行下面的分支。
        if (action == Action.CREATE) {
            // 准备数据（data），类型为 null；初始内容是调用 (com.fasterxml.jackson.databind.node.ObjectNode)command.path 完成路径处理。
            var data = (com.fasterxml.jackson.databind.node.ObjectNode) command.path("data").deepCopy();
            // 检查条件 data.hasNonNull("expectedTime") 是否成立；成立时执行下面的分支。
            if (data.hasNonNull("expectedTime")) {
                // 保存指定键对应的值。
                data.put("expectedTime", java.time.LocalTime.parse(data.path("expectedTime").asText())
                        // 继续对上一步结果执行调用 format(java.time.format.DateTimeFormatter.ofPattern 完成of匹配规则处理。
                        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
            }
            // 把调用 json.valueToTree 完成值ToTree处理交还给调用方，本方法到此结束。
            return json.valueToTree(habits.create(PlanWritePayloadUtils.saveDTO(data, HabitSaveDTO.class, json)));
        }
        // 检查条件 action != Action.CATEGORY 是否成立；成立时执行下面的分支。
        // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
        if (action != Action.CATEGORY) throw BusinessException.badRequest("不支持的习惯操作");
        // 准备名称（name），类型为 String；初始内容是调用 command.path 完成路径处理。
        String name = command.path("name").asText();
        // 准备查询条件（query），类型为 HabitQueryDTO；初始内容是新建 习惯查询条件（HabitQueryDTO） 实例。
        HabitQueryDTO query = new HabitQueryDTO();
        // 设置 长度或数量校验规则（Size） 的值。
        query.setSize(100);
        // 设置 Keyword 的值。
        query.setKeyword(name);
        // 准备当前页（page），类型为 int；初始内容是固定数值 1。
        // 逐次执行循环：for (int page = 1; ; page++)；每轮处理一条数据或推进一次检查。
        // 完成本次状态更新或调用：page++;。
        for (int page = 1; ; page++) {
            // 设置 当前页（Page） 的值。
            query.setPage(page);
            // 准备结果（result），类型为 null；初始内容是调用 categories.page 完成当前页处理。
            var result = categories.page(query);
            // 单条数据（item），使用 null 类型保存。
            // 逐次执行循环：for (/*missing* / item : result.items())；每轮处理一条数据或推进一次检查。
            for (var item : result.items()) {
                // 检查条件 name.equals(item.name()) 是否成立；成立时执行下面的分支。
                // 把调用 json.valueToTree 完成值ToTree处理交还给调用方，本方法到此结束。
                if (name.equals(item.name())) return json.valueToTree(item);
            }
            // 检查条件 long)page * 100 >= result.total( 是否成立；成立时执行下面的分支。
            if ((long) page * 100 >= result.total()) break;
        }
        // 把调用 json.valueToTree 完成值ToTree处理交还给调用方，本方法到此结束。
        return json.valueToTree(categories.create(new HabitCategorySaveDTO(name, null, null)));
    }
}
