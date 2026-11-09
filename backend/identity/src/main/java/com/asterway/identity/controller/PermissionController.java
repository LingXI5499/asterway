/*
 * 提供权限请求入口的 HTTP 入口，校验输入后交给业务服务处理。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.controller;

// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入结果（ResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.ResultVO;
// 引入权限查询条件（PermissionQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.PermissionQueryDTO;
// 引入权限保存（PermissionSaveDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.PermissionSaveDTO;
// 引入权限业务服务（PermissionService），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.service.PermissionService;
// 引入权限（PermissionVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.PermissionVO;
// 引入有效（Valid），下面可以直接使用这个类型或静态成员。
import jakarta.validation.Valid;
// 引入正数（Positive），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Positive;
// 引入非负数校验规则（PositiveOrZero），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.PositiveOrZero;
// 引入HTTP请求状态（HttpStatus），下面可以直接使用这个类型或静态成员。
import org.springframework.http.HttpStatus;
// 引入提前Authorize（PreAuthorize），下面可以直接使用这个类型或静态成员。
import org.springframework.security.access.prepost.PreAuthorize;
// 引入Validated，下面可以直接使用这个类型或静态成员。
import org.springframework.validation.annotation.Validated;
// 引入删除字段对应关系（DeleteMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.DeleteMapping;
// 引入读取字段对应关系（GetMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.GetMapping;
// 引入模型Attribute（ModelAttribute），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.ModelAttribute;
// 引入Patch字段对应关系（PatchMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.PatchMapping;
// 引入路径Variable（PathVariable），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.PathVariable;
// 引入Post字段对应关系（PostMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.PostMapping;
// 引入请求请求正文（RequestBody），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RequestBody;
// 引入请求字段对应关系（RequestMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RequestMapping;
// 引入请求参数（RequestParam），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RequestParam;
// 引入响应状态（ResponseStatus），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.ResponseStatus;
// 引入Rest请求入口（RestController），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RestController;

/* 动作权限管理链路，新增、修改与删除共用同一服务。 */
// 把这个类作为 HTTP 接口入口，并把返回对象编码成 JSON。
// 定义类 PermissionController；提供权限请求入口的 HTTP 入口，校验输入后交给业务服务处理。
@RestController
// 启用方法参数上的校验规则。
@Validated
// 设置这组接口共同使用的地址前缀或请求条件。
@RequestMapping("/asterway/permissions")
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class PermissionController {

    // 业务服务（service），使用 PermissionService 类型保存。
    private final PermissionService service;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 业务服务（service），使用 PermissionService 类型保存。
    public PermissionController(PermissionService service) {
        // 把业务服务（service）保存到 this.service。
        this.service = service;
    }

    /* 分页查询权限。 */
    // 把 GET 请求交给下面的方法，用来读取数据。
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    @GetMapping
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:permission:list')")
    // 继续检查这个对象内部字段上的校验规则。
    // 把查询参数按名称装入这个筛选或分页对象。
    // 查询条件（query），使用 PermissionQueryDTO 类型保存。
    public ResultVO<PageResultVO<PermissionVO>> page(@Valid @ModelAttribute PermissionQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.page(query));
    }

    /* 读取权限。 */
    // 把 GET 请求交给下面的方法，用来读取数据。
    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    @GetMapping("/{id}")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:permission:read')")
    // 从 URL 路径中读取这个参数，例如记录编号。
    // 要求数值大于零。
    // 编号（id），使用 long 类型保存。
    public ResultVO<PermissionVO> get(@PathVariable @Positive long id) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.get(id));
    }

    /* 新增扩展权限。 */
    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // 创建（create）方法：调用 create 完成创建处理；参数负责传入本次处理所需的数据。
    @PostMapping
    // 指定此方法成功时使用的 HTTP 状态码。
    @ResponseStatus(HttpStatus.CREATED)
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:permission:create')")
    // 继续检查这个对象内部字段上的校验规则。
    // 从请求 JSON 正文中读取这个参数对象。
    // 请求参数对象（dto），使用 PermissionSaveDTO 类型保存。
    public ResultVO<PermissionVO> create(@Valid @RequestBody PermissionSaveDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.create(dto));
    }

    /* 更新权限。 */
    // 把 PATCH 请求交给下面的方法，用来修改部分字段。
    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    @PatchMapping("/{id}")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:permission:update')")
    // 从 URL 路径中读取这个参数，例如记录编号。
    // 要求数值大于零。
    // 继续检查这个对象内部字段上的校验规则。
    // 从请求 JSON 正文中读取这个参数对象。
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 PermissionSaveDTO 类型保存。
    public ResultVO<PermissionVO> update(@PathVariable @Positive long id, @Valid @RequestBody PermissionSaveDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.update(id, dto));
    }

    /* 删除未引用的扩展权限。 */
    // 把 DELETE 请求交给下面的方法，具体删除规则由业务服务决定。
    // 删除（delete）方法：调用 delete 完成删除处理；参数负责传入本次处理所需的数据。
    @DeleteMapping("/{id}")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:permission:delete')")
    // 从 URL 路径中读取这个参数，例如记录编号。
    // 要求数值大于零。
    // 从 URL 查询参数中读取这个值。
    // 要求数值为零或正数。
    // 编号（id），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    public ResultVO<Void> delete(@PathVariable @Positive long id, @RequestParam @PositiveOrZero long version) {
        // 调用 service.delete 完成删除处理。
        service.delete(id, version);
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(null);
    }
}
