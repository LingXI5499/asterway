/*
 * 提供菜单请求入口的 HTTP 入口，校验输入后交给业务服务处理。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.controller;

// 引入结果（ResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.ResultVO;
// 引入菜单保存（MenuSaveDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.MenuSaveDTO;
// 引入菜单业务服务（MenuService），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.service.MenuService;
// 引入菜单（MenuVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.MenuVO;
// 引入有效（Valid），下面可以直接使用这个类型或静态成员。
import jakarta.validation.Valid;
// 引入正数（Positive），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Positive;
// 引入非负数校验规则（PositiveOrZero），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.PositiveOrZero;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;
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

/* 菜单与按钮的同一 HTTP 管理入口。 */
// 把这个类作为 HTTP 接口入口，并把返回对象编码成 JSON。
// 定义类 MenuController；提供菜单请求入口的 HTTP 入口，校验输入后交给业务服务处理。
@RestController
// 启用方法参数上的校验规则。
@Validated
// 设置这组接口共同使用的地址前缀或请求条件。
@RequestMapping("/asterway/menus")
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class MenuController {

    // 业务服务（service），使用 MenuService 类型保存。
    private final MenuService service;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 业务服务（service），使用 MenuService 类型保存。
    public MenuController(MenuService service) {
        // 把业务服务（service）保存到 this.service。
        this.service = service;
    }

    /* 读取平铺菜单。 */
    // 把 GET 请求交给下面的方法，用来读取数据。
    // 列表（list）方法：调用 list 完成列表处理；参数负责传入本次处理所需的数据。
    @GetMapping
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:menu:list')")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<List<MenuVO>> list() {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.list());
    }

    /* 读取管理菜单树。 */
    // 把 GET 请求交给下面的方法，用来读取数据。
    // tree方法：调用 tree 完成tree处理；参数负责传入本次处理所需的数据。
    @GetMapping("/tree")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:menu:list')")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<List<MenuVO>> tree() {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.tree());
    }

    /* 读取单个菜单。 */
    // 把 GET 请求交给下面的方法，用来读取数据。
    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    @GetMapping("/{id}")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:menu:read')")
    // 从 URL 路径中读取这个参数，例如记录编号。
    // 要求数值大于零。
    // 编号（id），使用 long 类型保存。
    public ResultVO<MenuVO> get(@PathVariable @Positive long id) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.get(id));
    }

    /* 新增目录、页面或按钮。 */
    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // 创建（create）方法：调用 create 完成创建处理；参数负责传入本次处理所需的数据。
    @PostMapping
    // 指定此方法成功时使用的 HTTP 状态码。
    @ResponseStatus(HttpStatus.CREATED)
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:menu:create')")
    // 继续检查这个对象内部字段上的校验规则。
    // 从请求 JSON 正文中读取这个参数对象。
    // 请求参数对象（dto），使用 MenuSaveDTO 类型保存。
    public ResultVO<MenuVO> create(@Valid @RequestBody MenuSaveDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.create(dto));
    }

    /* 更新菜单。 */
    // 把 PATCH 请求交给下面的方法，用来修改部分字段。
    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    @PatchMapping("/{id}")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:menu:update')")
    // 从 URL 路径中读取这个参数，例如记录编号。
    // 要求数值大于零。
    // 继续检查这个对象内部字段上的校验规则。
    // 从请求 JSON 正文中读取这个参数对象。
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 MenuSaveDTO 类型保存。
    public ResultVO<MenuVO> update(@PathVariable @Positive long id, @Valid @RequestBody MenuSaveDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.update(id, dto));
    }

    /* 删除未引用的扩展菜单。 */
    // 把 DELETE 请求交给下面的方法，具体删除规则由业务服务决定。
    // 删除（delete）方法：调用 delete 完成删除处理；参数负责传入本次处理所需的数据。
    @DeleteMapping("/{id}")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:menu:delete')")
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
