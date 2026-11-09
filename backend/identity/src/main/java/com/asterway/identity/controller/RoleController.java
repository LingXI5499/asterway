/*
 * 提供角色请求入口的 HTTP 入口，校验输入后交给业务服务处理。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.controller;

// 引入结果（ResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.ResultVO;
// 引入菜单Grant（MenuGrantDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.MenuGrantDTO;
// 引入权限Grant（PermissionGrantDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.PermissionGrantDTO;
// 引入角色更新（RoleUpdateDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.RoleUpdateDTO;
// 引入角色业务服务（RoleService），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.service.RoleService;
// 引入角色（RoleVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.RoleVO;
// 引入有效（Valid），下面可以直接使用这个类型或静态成员。
import jakarta.validation.Valid;
// 引入正数（Positive），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Positive;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;
// 引入提前Authorize（PreAuthorize），下面可以直接使用这个类型或静态成员。
import org.springframework.security.access.prepost.PreAuthorize;
// 引入Validated，下面可以直接使用这个类型或静态成员。
import org.springframework.validation.annotation.Validated;
// 引入读取字段对应关系（GetMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.GetMapping;
// 引入Patch字段对应关系（PatchMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.PatchMapping;
// 引入路径Variable（PathVariable），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.PathVariable;
// 引入Put字段对应关系（PutMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.PutMapping;
// 引入请求请求正文（RequestBody），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RequestBody;
// 引入请求字段对应关系（RequestMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RequestMapping;
// 引入Rest请求入口（RestController），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RestController;

/* 角色及其权限菜单授权共用一个 Controller，不按动作拆分。 */
// 把这个类作为 HTTP 接口入口，并把返回对象编码成 JSON。
// 定义类 RoleController；提供角色请求入口的 HTTP 入口，校验输入后交给业务服务处理。
@RestController
// 启用方法参数上的校验规则。
@Validated
// 设置这组接口共同使用的地址前缀或请求条件。
@RequestMapping("/asterway/roles")
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class RoleController {

    // 业务服务（service），使用 RoleService 类型保存。
    private final RoleService service;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 业务服务（service），使用 RoleService 类型保存。
    public RoleController(RoleService service) {
        // 把业务服务（service）保存到 this.service。
        this.service = service;
    }

    /* 查询三个内置角色。 */
    // 把 GET 请求交给下面的方法，用来读取数据。
    // 列表（list）方法：调用 list 完成列表处理；参数负责传入本次处理所需的数据。
    @GetMapping
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:role:list')")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<List<RoleVO>> list() {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.list());
    }

    /* 读取角色详情。 */
    // 把 GET 请求交给下面的方法，用来读取数据。
    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    @GetMapping("/{id}")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:role:read')")
    // 从 URL 路径中读取这个参数，例如记录编号。
    // 要求数值大于零。
    // 编号（id），使用 long 类型保存。
    public ResultVO<RoleVO> get(@PathVariable @Positive long id) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.get(id));
    }

    /* 修改角色展示信息。 */
    // 把 PATCH 请求交给下面的方法，用来修改部分字段。
    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    @PatchMapping("/{id}")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:role:update')")
    // 从 URL 路径中读取这个参数，例如记录编号。
    // 要求数值大于零。
    // 继续检查这个对象内部字段上的校验规则。
    // 从请求 JSON 正文中读取这个参数对象。
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 RoleUpdateDTO 类型保存。
    public ResultVO<RoleVO> update(@PathVariable @Positive long id, @Valid @RequestBody RoleUpdateDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.update(id, dto));
    }

    /* 读取角色动作权限集合。 */
    // 把 GET 请求交给下面的方法，用来读取数据。
    // 权限列表（permissions）方法：调用 permissions 完成权限列表处理；参数负责传入本次处理所需的数据。
    @GetMapping("/{id}/permissions")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:role:read')")
    // 从 URL 路径中读取这个参数，例如记录编号。
    // 要求数值大于零。
    // 编号（id），使用 long 类型保存。
    public ResultVO<List<String>> permissions(@PathVariable @Positive long id) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.permissions(id));
    }

    /* 替换角色动作权限集合。 */
    // 把 PUT 请求交给下面的方法，用来整体更新资源。
    // grant权限列表（grantPermissions）方法：调用 grantPermissions 完成grant权限列表处理；参数负责传入本次处理所需的数据。
    @PutMapping("/{id}/permissions")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:role:grant_permission')")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<RoleVO> grantPermissions(
            // 从 URL 路径中读取这个参数，例如记录编号。
            // 要求数值大于零。
            // 编号（id），使用 long 类型保存。
            @PathVariable @Positive long id,
            // 继续检查这个对象内部字段上的校验规则。
            // 从请求 JSON 正文中读取这个参数对象。
            // 请求参数对象（dto），使用 PermissionGrantDTO 类型保存。
            @Valid @RequestBody PermissionGrantDTO dto
    ) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.grantPermissions(id, dto));
    }

    /* 读取角色菜单集合。 */
    // 把 GET 请求交给下面的方法，用来读取数据。
    // 菜单列表（menus）方法：调用 menus 完成菜单列表处理；参数负责传入本次处理所需的数据。
    @GetMapping("/{id}/menus")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:role:read')")
    // 从 URL 路径中读取这个参数，例如记录编号。
    // 要求数值大于零。
    // 编号（id），使用 long 类型保存。
    public ResultVO<List<String>> menus(@PathVariable @Positive long id) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.menus(id));
    }

    /* 替换角色菜单并补齐祖先节点。 */
    // 把 PUT 请求交给下面的方法，用来整体更新资源。
    // grant菜单列表（grantMenus）方法：调用 grantMenus 完成grant菜单列表处理；参数负责传入本次处理所需的数据。
    @PutMapping("/{id}/menus")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:role:grant_menu')")
    // 从 URL 路径中读取这个参数，例如记录编号。
    // 要求数值大于零。
    // 继续检查这个对象内部字段上的校验规则。
    // 从请求 JSON 正文中读取这个参数对象。
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 MenuGrantDTO 类型保存。
    public ResultVO<RoleVO> grantMenus(@PathVariable @Positive long id, @Valid @RequestBody MenuGrantDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.grantMenus(id, dto));
    }
}
