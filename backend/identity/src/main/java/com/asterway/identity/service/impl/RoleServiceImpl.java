/*
 * 实现角色业务服务业务规则，处理权限、状态、数据读写和必要的事务。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.service.impl;

// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入账号ValidationUtils（AccountValidationUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.AccountValidationUtils;
// 引入SecurityUtils，下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.SecurityUtils;
// 引入菜单Grant（MenuGrantDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.MenuGrantDTO;
// 引入权限Grant（PermissionGrantDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.PermissionGrantDTO;
// 引入角色更新（RoleUpdateDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.RoleUpdateDTO;
// 引入菜单（MenuEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.entity.MenuEntity;
// 引入权限（PermissionEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.entity.PermissionEntity;
// 引入角色（RoleEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.entity.RoleEntity;
// 引入菜单数据库读写器（MenuMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.mapper.MenuMapper;
// 引入权限数据库读写器（PermissionMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.mapper.PermissionMapper;
// 引入角色数据库读写器（RoleMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.mapper.RoleMapper;
// 引入角色菜单数据库读写器（RoleMenuMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.mapper.RoleMenuMapper;
// 引入角色权限数据库读写器（RolePermissionMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.mapper.RolePermissionMapper;
// 引入操作日志业务服务（OperationLogService），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.service.OperationLogService;
// 引入角色业务服务（RoleService），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.service.RoleService;
// 引入角色（RoleVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.RoleVO;

// 引入业务服务（Service），下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Service;
// 引入Transactional，下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.annotation.Transactional;

// 引入摘要值对应表（HashMap），下面可以直接使用这个类型或静态成员。
import java.util.HashMap;
// 引入摘要值设置（HashSet），下面可以直接使用这个类型或静态成员。
import java.util.HashSet;
// 引入Linked摘要值设置（LinkedHashSet），下面可以直接使用这个类型或静态成员。
import java.util.LinkedHashSet;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;
// 引入按键保存值的对应表（Map），下面可以直接使用这个类型或静态成员。
import java.util.Map;
// 引入设置（Set），下面可以直接使用这个类型或静态成员。
import java.util.Set;

/* 角色展示与授权集中处理；关联表在同一事务内替换，不创建额外用例层。 */
// 把该实现交给 Spring 管理，其他组件可以注入并调用。
// 定义类 RoleServiceImpl；实现角色业务服务业务规则，处理权限、状态、数据读写和必要的事务。
@Service
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class RoleServiceImpl implements RoleService {

    // 访问数据库的 Mapper（mapper），使用 RoleMapper 类型保存。
    private final RoleMapper mapper;

    // 权限列表（permissions），使用 PermissionMapper 类型保存。
    private final PermissionMapper permissions;

    // 菜单列表（menus），使用 MenuMapper 类型保存。
    private final MenuMapper menus;

    // 角色权限列表（rolePermissions），使用 RolePermissionMapper 类型保存。
    private final RolePermissionMapper rolePermissions;

    // 角色菜单列表（roleMenus），使用 RoleMenuMapper 类型保存。
    private final RoleMenuMapper roleMenus;

    // logs，使用 OperationLogService 类型保存。
    private final OperationLogService logs;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    public RoleServiceImpl(
            // 访问数据库的 Mapper（mapper），使用 RoleMapper 类型保存。
            RoleMapper mapper,
            // 权限列表（permissions），使用 PermissionMapper 类型保存。
            PermissionMapper permissions,
            // 菜单列表（menus），使用 MenuMapper 类型保存。
            MenuMapper menus,
            // 角色权限列表（rolePermissions），使用 RolePermissionMapper 类型保存。
            RolePermissionMapper rolePermissions,
            // 角色菜单列表（roleMenus），使用 RoleMenuMapper 类型保存。
            RoleMenuMapper roleMenus,
            // logs，使用 OperationLogService 类型保存。
            OperationLogService logs) {
        // 把访问数据库的 Mapper（mapper）保存到 this.mapper。
        this.mapper = mapper;
        // 把权限列表（permissions）保存到 this.permissions。
        this.permissions = permissions;
        // 把菜单列表（menus）保存到 this.menus。
        this.menus = menus;
        // 把角色权限列表（rolePermissions）保存到 this.rolePermissions。
        this.rolePermissions = rolePermissions;
        // 把角色菜单列表（roleMenus）保存到 this.roleMenus。
        this.roleMenus = roleMenus;
        // 把logs保存到 this.logs。
        this.logs = logs;
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 列表（list）方法：调用 list 完成列表处理；参数负责传入本次处理所需的数据。
    @Override
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public List<RoleVO> list() {
        // 要求满足 SuperAdmin。
        SecurityUtils.requireSuperAdmin("identity:role:list");
        // 把把前面筛选、转换后的结果汇成列表交还给调用方，本方法到此结束。
        return mapper.findAll().stream()
                // 当前记录（row），使用 null 类型保存。
                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                .filter(row -> Boolean.TRUE.equals(row.getBuiltin()))
                // 继续对上一步结果执行把每个条目转换成需要的形式。
                .map(this::toVO)
                // 继续对上一步结果执行调用 toList 完成to列表处理。
                .toList();
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    @Override
    // 编号（id），使用 long 类型保存。
    public RoleVO get(long id) {
        // 要求满足 SuperAdmin。
        SecurityUtils.requireSuperAdmin("identity:role:read");
        // 把把数据库记录转换为可以返回页面的对象交还给调用方，本方法到此结束。
        return toVO(required(id));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 RoleUpdateDTO 类型保存。
    public RoleVO update(long id, RoleUpdateDTO dto) {
        // 准备actor，类型为 null；初始内容是要求满足 SuperAdmin。
        var actor = SecurityUtils.requireSuperAdmin("identity:role:update");
        // 读取并锁定符合条件的记录，具体 SQL 对应 lockSecurityGate。
        mapper.lockSecurityGate();
        // 准备当前记录（row），类型为 RoleEntity；初始内容是读取必须存在的记录，不存在时给出业务错误。
        RoleEntity row = required(id);
        // 调用 AccountValidationUtils.version 完成版本号处理。
        AccountValidationUtils.version(dto.version(), row.getVersion());
        // 设置 名称（Name） 的值。
        row.setName(dto.name());
        // 设置 说明（Description） 的值。
        row.setDescription(dto.description() == null ? "" : dto.description());
        // 设置 更新时按（UpdatedBy） 的值。
        row.setUpdatedBy(actor.userId());
        // 检查条件 mapper.update(row) != 1 是否成立；成立时执行下面的分支。
        if (mapper.update(row) != 1) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("角色已被修改，请刷新");
        }
        // 把新条目放到列表末尾。
        logs.append(
                // 继续传入或组合固定文字 'identity:role:update'，完成上面尚未结束的调用或声明。
                "identity:role:update",
                // 继续传入或组合固定状态或编码 ROLE（角色），完成上面尚未结束的调用或声明。
                "ROLE",
                // 调用 Long.toString 完成to文字处理。
                Long.toString(id),
                // 继续传入或组合空值，表示当前还没有对应内容，完成上面尚未结束的调用或声明。
                null,
                // 调用 Map.of 完成of处理。
                Map.of("version", dto.version()));
        // 把把数据库记录转换为可以返回页面的对象交还给调用方，本方法到此结束。
        return toVO(required(id));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 权限列表（permissions）方法：调用 permissions 完成权限列表处理；参数负责传入本次处理所需的数据。
    @Override
    // 编号（id），使用 long 类型保存。
    public List<String> permissions(long id) {
        // 要求满足 SuperAdmin。
        SecurityUtils.requireSuperAdmin("identity:role:read");
        // 读取必须存在的记录，不存在时给出业务错误。
        required(id);
        // 把把前面筛选、转换后的结果汇成列表交还给调用方，本方法到此结束。
        return rolePermissions.findIds(id).stream().map(Object::toString).toList();
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 菜单列表（menus）方法：调用 menus 完成菜单列表处理；参数负责传入本次处理所需的数据。
    @Override
    // 编号（id），使用 long 类型保存。
    public List<String> menus(long id) {
        // 要求满足 SuperAdmin。
        SecurityUtils.requireSuperAdmin("identity:role:read");
        // 读取必须存在的记录，不存在时给出业务错误。
        required(id);
        // 把把前面筛选、转换后的结果汇成列表交还给调用方，本方法到此结束。
        return roleMenus.findIds(id).stream().map(Object::toString).toList();
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // grant权限列表（grantPermissions）方法：调用 grantPermissions 完成grant权限列表处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 PermissionGrantDTO 类型保存。
    public RoleVO grantPermissions(long id, PermissionGrantDTO dto) {
        // 准备actor，类型为 null；初始内容是要求满足 SuperAdmin。
        var actor = SecurityUtils.requireSuperAdmin("identity:role:grant_permission");
        // 读取并锁定符合条件的记录，具体 SQL 对应 lockSecurityGate。
        mapper.lockSecurityGate();
        // 准备角色（role），类型为 RoleEntity；初始内容是读取必须存在的记录，不存在时给出业务错误。
        RoleEntity role = required(id);
        // 调用 AccountValidationUtils.version 完成版本号处理。
        AccountValidationUtils.version(dto.version(), role.getVersion());
        // 调用 AccountValidationUtils.distinctIds 完成distinct编号列表处理。
        AccountValidationUtils.distinctIds(dto.permissionIds());
        // 准备是否可用（available），类型为 Map<Long, PermissionEntity>；初始内容是新建 摘要值对应表（HashMap） 实例。
        Map<Long, PermissionEntity> available = new HashMap<>();
        // 当前记录（row），使用 null 类型保存。
        // 查找 全部（All）。
        // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
        permissions.findAll().forEach(row -> available.put(row.getId(), row));
        // 权限编号（permissionId），使用 Long 类型保存；可为 null，表示尚未提供。
        // 逐次执行循环：for (Long permissionId : dto.permissionIds())；每轮处理一条数据或推进一次检查。
        for (Long permissionId : dto.permissionIds()) {
            // 准备权限（permission），类型为 PermissionEntity；初始内容是按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            PermissionEntity permission = available.get(permissionId);
            // 检查当前用户是否具有需要的权限：permission == null || !Boolean.TRUE.equals(permission.getEnabled()) || !allowed(role.getCode(), permission.getCode())；成立时执行下面的分支。
            if (permission == null
                    // 继续补充同一个判断或备用结果：当前用户是否具有需要的权限：|| !Boolean.TRUE.equals(permission.getEnabled())。
                    || !Boolean.TRUE.equals(permission.getEnabled())
                    // 继续补充同一个判断或备用结果：当前用户是否具有需要的权限：|| !allowed(role.getCode(), permission.getCode())) {。
                    || !allowed(role.getCode(), permission.getCode())) {
                // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
                throw BusinessException.badRequest("权限不存在、已停用或超出该角色的管理边界");
            }
        }
        // 准备已选（selected），类型为 Set<Long>；初始内容是新建 摘要值设置（HashSet） 实例。
        Set<Long> selected = new HashSet<>(dto.permissionIds());
        // 权限（permission），使用 PermissionEntity 类型保存。
        // 逐次执行循环：for (PermissionEntity permission : available.values())；每轮处理一条数据或推进一次检查。
        for (PermissionEntity permission : available.values()) {
            // 准备mandatory，类型为 boolean；初始内容是优先使用前面的有效值，没有时采用后面的备用值：Boolean.TRUE.equals(permission.getBuiltin()) && ("SUPER_ADMIN".equals(role.getCode()) && !com.asterway.common.…。
            boolean mandatory =
                    // 比较两个值是否相同。
                    Boolean.TRUE.equals(permission.getBuiltin())
                            // 继续补充同一个判断或备用结果：条件 && ("SUPER_ADMIN".equals(role.getCode()) 是否成立。
                            && ("SUPER_ADMIN".equals(role.getCode())
                                            // 继续补充同一个判断或备用结果：条件 && !com.asterway.common.utils.GrowthAccessUtils 是否成立。
                                            && !com.asterway.common.utils.GrowthAccessUtils
                                                    // 继续对上一步结果执行读取 编码（Code） 的值。
                                                    .permission(permission.getCode())
                                    // 继续补充同一个判断或备用结果：条件 || "USER".equals(role.getCode()) 是否成立。
                                    || "USER".equals(role.getCode())
                                            // 继续补充同一个判断或备用结果：当前用户是否具有需要的权限：&& personal(permission.getCode()));。
                                            && personal(permission.getCode()));
            // 检查当前用户是否具有需要的权限：mandatory && !selected.contains(permission.getId())；成立时执行下面的分支。
            if (mandatory && !selected.contains(permission.getId())) {
                // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
                throw BusinessException.conflict("不能移除该内置角色的核心账户权限");
            }
        }
        // 调用 advance(role, actor.userId 完成用户编号处理。
        advance(role, actor.userId());
        // 删除 按所属人（ByOwner）。
        rolePermissions.deleteByOwner(id);
        // 权限编号（permissionId），使用 Long 类型保存；可为 null，表示尚未提供。
        // 逐次执行循环：for (Long permissionId : dto.permissionIds())；每轮处理一条数据或推进一次检查。
        for (Long permissionId : dto.permissionIds()) {
            // 调用 rolePermissions.insert 完成写入新记录处理。
            rolePermissions.insert(id, permissionId, actor.userId());
        }
        // 把新条目放到列表末尾。
        logs.append(
                // 继续传入或组合固定文字 'identity:role:grant_permission'，完成上面尚未结束的调用或声明。
                "identity:role:grant_permission",
                // 继续传入或组合固定状态或编码 ROLE（角色），完成上面尚未结束的调用或声明。
                "ROLE",
                // 调用 Long.toString 完成to文字处理。
                Long.toString(id),
                // 继续传入或组合空值，表示当前还没有对应内容，完成上面尚未结束的调用或声明。
                null,
                // 调用 Map.of 完成of处理。
                Map.of("permissionIds", dto.permissionIds()));
        // 把把数据库记录转换为可以返回页面的对象交还给调用方，本方法到此结束。
        return toVO(required(id));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // grant菜单列表（grantMenus）方法：调用 grantMenus 完成grant菜单列表处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 MenuGrantDTO 类型保存。
    public RoleVO grantMenus(long id, MenuGrantDTO dto) {
        // 准备actor，类型为 null；初始内容是要求满足 SuperAdmin。
        var actor = SecurityUtils.requireSuperAdmin("identity:role:grant_menu");
        // 读取并锁定符合条件的记录，具体 SQL 对应 lockSecurityGate。
        mapper.lockSecurityGate();
        // 准备角色（role），类型为 RoleEntity；初始内容是读取必须存在的记录，不存在时给出业务错误。
        RoleEntity role = required(id);
        // 调用 AccountValidationUtils.version 完成版本号处理。
        AccountValidationUtils.version(dto.version(), role.getVersion());
        // 调用 AccountValidationUtils.distinctIds 完成distinct编号列表处理。
        AccountValidationUtils.distinctIds(dto.menuIds());
        // 准备是否可用（available），类型为 Map<Long, MenuEntity>；初始内容是新建 摘要值对应表（HashMap） 实例。
        Map<Long, MenuEntity> available = new HashMap<>();
        // 当前记录（row），使用 null 类型保存。
        // 查找 全部（All）。
        // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
        menus.findAll().forEach(row -> available.put(row.getId(), row));
        // 准备成长业务权限编号列表（growthPermissionIds），类型为 Set<Long>；初始内容是新建 摘要值设置（HashSet） 实例。
        Set<Long> growthPermissionIds = new HashSet<>();
        // 查找 全部（All）。
        permissions.findAll().stream()
                // p，使用 null 类型保存。
                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                .filter(p -> com.asterway.common.utils.GrowthAccessUtils.permission(p.getCode()))
                // p，使用 null 类型保存。
                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                .forEach(p -> growthPermissionIds.add(p.getId()));
        // 准备已选（selected），类型为 Set<Long>；初始内容是新建 Linked摘要值设置（LinkedHashSet） 实例。
        Set<Long> selected = new LinkedHashSet<>();
        // 准备permitted，类型为 Set<Long>；初始内容是新建 摘要值设置（HashSet） 实例。
        Set<Long> permitted = new HashSet<>(rolePermissions.findIds(id));
        // 菜单编号（menuId），使用 Long 类型保存；可为 null，表示尚未提供。
        // 逐次执行循环：for (Long menuId : dto.menuIds())；每轮处理一条数据或推进一次检查。
        for (Long menuId : dto.menuIds()) {
            // 准备菜单（menu），类型为 MenuEntity；初始内容是按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            MenuEntity menu = available.get(menuId);
            // 准备chain，类型为 Set<Long>；初始内容是新建 摘要值设置（HashSet） 实例。
            Set<Long> chain = new HashSet<>();
            // 逐次执行循环：while (menu != null)；每轮处理一条数据或推进一次检查。
            while (menu != null) {
                // 检查当前用户是否具有需要的权限：!"USER".equals(role.getCode()) && (com.asterway.common.utils.GrowthAccessUtils.menu(menu.getCode(), menu.getComponentKey()) || growthPermissionId…；成立时执行下面的分支。
                if (!"USER".equals(role.getCode())
                        // 继续补充同一个判断或备用结果：条件 && (com.asterway.common.utils.GrowthAccessUtils.menu( 是否成立。
                        && (com.asterway.common.utils.GrowthAccessUtils.menu(
                                        // 读取 编码（Code） 的值。
                                        menu.getCode(), menu.getComponentKey())
                                // 继续补充同一个判断或备用结果：当前用户是否具有需要的权限：|| growthPermissionIds.contains(menu.getPermissionId()))) {。
                                || growthPermissionIds.contains(menu.getPermissionId()))) {
                    // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
                    throw BusinessException.badRequest("成长业务菜单只能分配给普通用户角色");
                }
                // 检查条件 !chain.add(menu.getId()) || !Boolean.TRUE.equals(menu.getEnabled()) 是否成立；成立时执行下面的分支。
                if (!chain.add(menu.getId()) || !Boolean.TRUE.equals(menu.getEnabled())) {
                    // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
                    throw BusinessException.badRequest("菜单父链循环或已停用");
                }
                // 检查当前用户是否具有需要的权限：menu.getPermissionId() != null && !"SUPER_ADMIN".equals(role.getCode()) && !permitted.contains(menu.getPermissionId())；成立时执行下面的分支。
                if (menu.getPermissionId() != null
                        // 继续补充同一个判断或备用结果：条件 && !"SUPER_ADMIN".equals(role.getCode()) 是否成立。
                        && !"SUPER_ADMIN".equals(role.getCode())
                        // 继续补充同一个判断或备用结果：当前用户是否具有需要的权限：&& !permitted.contains(menu.getPermissionId())) {。
                        && !permitted.contains(menu.getPermissionId())) {
                    // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
                    throw BusinessException.badRequest("请先授予菜单对应的动作权限");
                }
                // 加入集合或登记一条待保存记录。
                selected.add(menu.getId());
                // 检查menu.getParentId() == null，即对应内容是否不存在；成立时执行下面的分支。
                if (menu.getParentId() == null) {
                    // 继续传入或组合break，完成上面尚未结束的调用或声明。
                    break;
                }
                // 把按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值保存到 menu。
                menu = available.get(menu.getParentId());
            }
            // 检查menu == null，即对应内容是否不存在；成立时执行下面的分支。
            if (menu == null) {
                // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
                throw BusinessException.badRequest("菜单或父节点不存在");
            }
        }
        // 菜单（menu），使用 MenuEntity 类型保存。
        // 逐次执行循环：for (MenuEntity menu : available.values())；每轮处理一条数据或推进一次检查。
        for (MenuEntity menu : available.values()) {
            // 准备mandatory，类型为 boolean；初始内容是比较两个值是否相同。
            boolean mandatory =
                    // 比较两个值是否相同。
                    Boolean.TRUE.equals(menu.getBuiltin())
                            // 继续补充同一个判断或备用结果：条件 && ("SUPER_ADMIN".equals(role.getCode()) 是否成立。
                            && ("SUPER_ADMIN".equals(role.getCode())
                                            // 继续补充同一个判断或备用结果：条件 && !com.asterway.common.utils.GrowthAccessUtils.menu( 是否成立。
                                            && !com.asterway.common.utils.GrowthAccessUtils.menu(
                                                    // 读取 编码（Code） 的值。
                                                    menu.getCode(), menu.getComponentKey())
                                            // 继续补充同一个判断或备用结果：当前用户是否具有需要的权限：&& !growthPermissionIds.contains(menu.getPermissionId())。
                                            && !growthPermissionIds.contains(menu.getPermissionId())
                                    // 继续补充同一个判断或备用结果：条件 || "USER".equals(role.getCode()) 是否成立。
                                    || "USER".equals(role.getCode())
                                            // 继续补充同一个判断或备用结果：条件 && Set.of("account_root", "profile", "devices") 是否成立。
                                            && Set.of("account_root", "profile", "devices")
                                                    // 继续对上一步结果执行读取 编码（Code） 的值。
                                                    .contains(menu.getCode()));
            // 检查条件 mandatory && !selected.contains(menu.getId()) 是否成立；成立时执行下面的分支。
            if (mandatory && !selected.contains(menu.getId())) {
                // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
                throw BusinessException.conflict("不能移除该内置角色的核心菜单");
            }
        }
        // 调用 advance(role, actor.userId 完成用户编号处理。
        advance(role, actor.userId());
        // 删除 按所属人（ByOwner）。
        roleMenus.deleteByOwner(id);
        // 菜单编号（menuId），使用 Long 类型保存；可为 null，表示尚未提供。
        // 逐次执行循环：for (Long menuId : selected)；每轮处理一条数据或推进一次检查。
        for (Long menuId : selected) {
            // 调用 roleMenus.insert 完成写入新记录处理。
            roleMenus.insert(id, menuId, actor.userId());
        }
        // 把新条目放到列表末尾。
        logs.append(
                // 继续传入或组合固定文字 'identity:role:grant_menu'，完成上面尚未结束的调用或声明。
                "identity:role:grant_menu",
                // 继续传入或组合固定状态或编码 ROLE（角色），完成上面尚未结束的调用或声明。
                "ROLE",
                // 调用 Long.toString 完成to文字处理。
                Long.toString(id),
                // 继续传入或组合空值，表示当前还没有对应内容，完成上面尚未结束的调用或声明。
                null,
                // 调用 Map.of 完成of处理。
                Map.of("menuIds", selected));
        // 把把数据库记录转换为可以返回页面的对象交还给调用方，本方法到此结束。
        return toVO(required(id));
    }

    // 个人（personal）方法：调用 personal 完成个人处理；参数负责传入本次处理所需的数据。
    // 编码（code），使用 String 类型保存；可为 null，表示尚未提供。
    private boolean personal(String code) {
        // 把判断文字是否具有指定前缀交还给调用方，本方法到此结束。
        return code.startsWith("identity:")
                // 继续补充同一个判断或备用结果：条件 && Set.of("profile", "password", "timezone", "device").contains(code.split(":")[1]); 是否成立。
                && Set.of("profile", "password", "timezone", "device").contains(code.split(":")[1]);
    }

    // allowed方法：调用 allowed 完成allowed处理；参数负责传入本次处理所需的数据。
    // 角色（role），使用 String 类型保存；可为 null，表示尚未提供。
    // 编码（code），使用 String 类型保存；可为 null，表示尚未提供。
    private boolean allowed(String role, String code) {
        // 检查当前用户是否具有需要的权限：com.asterway.common.utils.GrowthAccessUtils.permission(code)；成立时执行下面的分支。
        if (com.asterway.common.utils.GrowthAccessUtils.permission(code)) {
            // 把比较两个值是否相同交还给调用方，本方法到此结束。
            return "USER".equals(role)
                    // 继续补充同一个判断或备用结果：当前用户是否具有需要的权限：&& com.asterway.common.constant.GrowthPermissionConstants.CODES.contains(code);。
                    && com.asterway.common.constant.GrowthPermissionConstants.CODES.contains(code);
        }
        // 检查条件 "SUPER_ADMIN".equals(role) 是否成立；成立时执行下面的分支。
        if ("SUPER_ADMIN".equals(role)) {
            // 把真，表示条件成立或开关打开交还给调用方，本方法到此结束。
            return true;
        }
        // 检查条件 !code.startsWith("identity:") 是否成立；成立时执行下面的分支。
        if (!code.startsWith("identity:")) {
            // 把判断集合是否包含指定内容交还给调用方，本方法到此结束。
            return com.asterway.common.constant.GrowthPermissionConstants.CODES.contains(code);
        }
        // 检查条件 personal(code) 是否成立；成立时执行下面的分支。
        if (personal(code)) {
            // 把真，表示条件成立或开关打开交还给调用方，本方法到此结束。
            return true;
        }
        // 把优先使用前面的有效值，没有时采用后面的备用值："ADMIN".equals(role) && (code.startsWith("identity:user:") && !code.endsWith(":assign_role") || Set.of("sessio…交还给调用方，本方法到此结束。
        return "ADMIN".equals(role)
                // 继续补充同一个判断或备用结果：条件 && (code.startsWith("identity:user:") && !code.endsWith(":assign_role") 是否成立。
                && (code.startsWith("identity:user:") && !code.endsWith(":assign_role")
                        // 继续补充同一个判断或备用结果：条件 || Set.of("session", "login_log", "operation_log") 是否成立。
                        || Set.of("session", "login_log", "operation_log")
                                // 继续对上一步结果执行按指定分隔规则拆分文字。
                                .contains(code.split(":")[1]));
    }

    // advance方法：调用 advance 完成advance处理；参数负责传入本次处理所需的数据。
    // 角色（role），使用 RoleEntity 类型保存。
    // actor编号（actorId），使用 long 类型保存。
    private void advance(RoleEntity role, long actorId) {
        // 检查当前记录版本是否符合请求要求：mapper.advanceVersion(role.getId(), role.getVersion(), actorId) != 1；成立时执行下面的分支。
        if (mapper.advanceVersion(role.getId(), role.getVersion(), actorId) != 1) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("角色授权已变化，请刷新");
        }
    }

    // 必填（required）方法：读取必须存在的记录，不存在时给出业务错误；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    private RoleEntity required(long id) {
        // 准备角色（role），类型为 RoleEntity；初始内容是从数据库读取符合条件的记录，具体 SQL 对应 findById。
        RoleEntity role = mapper.findById(id);
        // 检查role == null || !Boolean.TRUE.equals(role.getBuiltin())，即对应内容是否不存在；成立时执行下面的分支。
        if (role == null || !Boolean.TRUE.equals(role.getBuiltin())) {
            // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
            throw BusinessException.notFound();
        }
        // 把角色（role）交还给调用方，本方法到此结束。
        return role;
    }

    // 转换为页面需要的返回对象（toVO）方法：把数据库记录转换为可以返回页面的对象；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 RoleEntity 类型保存。
    private RoleVO toVO(RoleEntity row) {
        // 把新建 角色（RoleVO） 实例交还给调用方，本方法到此结束。
        return new RoleVO(
                // 读取 编号（Id） 的值。
                row.getId().toString(),
                // 读取 编码（Code） 的值。
                row.getCode(),
                // 读取 名称（Name） 的值。
                row.getName(),
                // 读取 说明（Description） 的值。
                row.getDescription(),
                // 比较两个值是否相同。
                Boolean.TRUE.equals(row.getBuiltin()),
                // 比较两个值是否相同。
                Boolean.TRUE.equals(row.getEnabled()),
                // 读取 版本号（Version） 的值。
                row.getVersion(),
                // 把前面筛选、转换后的结果汇成列表。
                rolePermissions.findIds(row.getId()).stream().map(Object::toString).toList(),
                // 把前面筛选、转换后的结果汇成列表。
                roleMenus.findIds(row.getId()).stream().map(Object::toString).toList());
    }
}
