/*
 * 实现菜单业务服务业务规则，处理权限、状态、数据读写和必要的事务。
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
// 引入菜单保存（MenuSaveDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.MenuSaveDTO;
// 引入菜单（MenuEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.entity.MenuEntity;
// 引入权限（PermissionEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.entity.PermissionEntity;
// 引入菜单数据库读写器（MenuMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.mapper.MenuMapper;
// 引入权限数据库读写器（PermissionMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.mapper.PermissionMapper;
// 引入角色数据库读写器（RoleMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.mapper.RoleMapper;
// 引入菜单业务服务（MenuService），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.service.MenuService;
// 引入操作日志业务服务（OperationLogService），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.service.OperationLogService;
// 引入菜单（MenuVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.MenuVO;

// 引入业务服务（Service），下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Service;
// 引入Transactional，下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.annotation.Transactional;

// 引入Array列表（ArrayList），下面可以直接使用这个类型或静态成员。
import java.util.ArrayList;
// 引入摘要值对应表（HashMap），下面可以直接使用这个类型或静态成员。
import java.util.HashMap;
// 引入摘要值设置（HashSet），下面可以直接使用这个类型或静态成员。
import java.util.HashSet;
// 引入Linked摘要值对应表（LinkedHashMap），下面可以直接使用这个类型或静态成员。
import java.util.LinkedHashMap;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;
// 引入按键保存值的对应表（Map），下面可以直接使用这个类型或静态成员。
import java.util.Map;
// 引入设置（Set），下面可以直接使用这个类型或静态成员。
import java.util.Set;
// 引入Collectors，下面可以直接使用这个类型或静态成员。
import java.util.stream.Collectors;

/* 菜单管理与当前授权菜单共用同一链路，不由前端决定实际接口权限。 */
// 把该实现交给 Spring 管理，其他组件可以注入并调用。
// 定义类 MenuServiceImpl；实现菜单业务服务业务规则，处理权限、状态、数据读写和必要的事务。
@Service
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class MenuServiceImpl implements MenuService {

    // 访问数据库的 Mapper（mapper），使用 MenuMapper 类型保存。
    private final MenuMapper mapper;

    // 权限列表（permissions），使用 PermissionMapper 类型保存。
    private final PermissionMapper permissions;

    // 角色列表（roles），使用 RoleMapper 类型保存。
    private final RoleMapper roles;

    // logs，使用 OperationLogService 类型保存。
    private final OperationLogService logs;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    public MenuServiceImpl(
            // 访问数据库的 Mapper（mapper），使用 MenuMapper 类型保存。
            MenuMapper mapper,
            // 权限列表（permissions），使用 PermissionMapper 类型保存。
            PermissionMapper permissions,
            // 角色列表（roles），使用 RoleMapper 类型保存。
            RoleMapper roles,
            // logs，使用 OperationLogService 类型保存。
            OperationLogService logs) {
        // 把访问数据库的 Mapper（mapper）保存到 this.mapper。
        this.mapper = mapper;
        // 把权限列表（permissions）保存到 this.permissions。
        this.permissions = permissions;
        // 把角色列表（roles）保存到 this.roles。
        this.roles = roles;
        // 把logs保存到 this.logs。
        this.logs = logs;
    }

    // 准备COMPONENTS，类型为 Set<String>；初始内容是调用 Set.of 完成of处理。
    private static final Set<String> COMPONENTS =
            // 继续传入或组合调用 Set.of 完成of处理，完成上面尚未结束的调用或声明。
            Set.of(
                    // 继续传入或组合固定文字 'account/profile'，完成上面尚未结束的调用或声明。
                    "account/profile",
                    // 继续传入或组合固定文字 'account/devices'，完成上面尚未结束的调用或声明。
                    "account/devices",
                    // 继续传入或组合固定文字 'system/users'，完成上面尚未结束的调用或声明。
                    "system/users",
                    // 继续传入或组合固定文字 'system/roles'，完成上面尚未结束的调用或声明。
                    "system/roles",
                    // 继续传入或组合固定文字 'system/permissions'，完成上面尚未结束的调用或声明。
                    "system/permissions",
                    // 继续传入或组合固定文字 'system/menus'，完成上面尚未结束的调用或声明。
                    "system/menus",
                    // 继续传入或组合固定文字 'system/sessions'，完成上面尚未结束的调用或声明。
                    "system/sessions",
                    // 继续传入或组合固定文字 'system/login_logs'，完成上面尚未结束的调用或声明。
                    "system/login_logs",
                    // 继续传入或组合固定文字 'system/operation_logs'，完成上面尚未结束的调用或声明。
                    "system/operation_logs",
                    // 继续传入或组合固定文字 'system/account_settings'，完成上面尚未结束的调用或声明。
                    "system/account_settings",
                    // 继续传入或组合固定文字 'growth/goals'，完成上面尚未结束的调用或声明。
                    "growth/goals",
                    // 继续传入或组合固定文字 'growth/stages'，完成上面尚未结束的调用或声明。
                    "growth/stages",
                    // 继续传入或组合固定文字 'growth/tasks'，完成上面尚未结束的调用或声明。
                    "growth/tasks",
                    // 继续传入或组合固定文字 'planning/weekly'，完成上面尚未结束的调用或声明。
                    "planning/weekly",
                    // 继续传入或组合固定文字 'planning/daily'，完成上面尚未结束的调用或声明。
                    "planning/daily",
                    // 继续传入或组合固定文字 'planning/pending'，完成上面尚未结束的调用或声明。
                    "planning/pending",
                    // 继续传入或组合固定文字 'execution/index'，完成上面尚未结束的调用或声明。
                    "execution/index",
                    // 继续传入或组合固定文字 'habit/index'，完成上面尚未结束的调用或声明。
                    "habit/index",
                    // 继续传入或组合固定文字 'habit/categories'，完成上面尚未结束的调用或声明。
                    "habit/categories",
                    // 继续传入或组合固定文字 'notification/index'，完成上面尚未结束的调用或声明。
                    "notification/index",
                    // 继续传入或组合固定文字 'dashboard/index'，完成上面尚未结束的调用或声明。
                    "dashboard/index",
                    // 继续传入或组合固定文字 'statistics/index'，完成上面尚未结束的调用或声明。
                    "statistics/index",
                    // 继续传入或组合固定文字 'agent/index'，完成上面尚未结束的调用或声明。
                    "agent/index",
                    // 继续传入或组合固定文字 'agent/memories'，完成上面尚未结束的调用或声明。
                    "agent/memories",
                    // 继续传入或组合固定文字 'agent/knowledge'，完成上面尚未结束的调用或声明。
                    "agent/knowledge",
                    // 继续传入或组合固定文字 'agent/runs'，完成上面尚未结束的调用或声明。
                    "agent/runs",
                    // 继续传入或组合按表达式计算得到的值："agent/reviews")，完成上面尚未结束的调用或声明。
                    "agent/reviews");

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 列表（list）方法：调用 list 完成列表处理；参数负责传入本次处理所需的数据。
    @Override
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public List<MenuVO> list() {
        // 要求满足 SuperAdmin。
        SecurityUtils.requireSuperAdmin("identity:menu:list");
        // 把把前面筛选、转换后的结果汇成列表交还给调用方，本方法到此结束。
        return mapper.findAll().stream().map(this::toVO).toList();
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // tree方法：调用 tree 完成tree处理；参数负责传入本次处理所需的数据。
    @Override
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public List<MenuVO> tree() {
        // 要求满足 SuperAdmin。
        SecurityUtils.requireSuperAdmin("identity:menu:list");
        // 把从数据库读取符合条件的记录，具体 SQL 对应 findAll交还给调用方，本方法到此结束。
        return buildTree(mapper.findAll());
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 当前Tree（currentTree）方法：调用 currentTree 完成当前Tree处理；参数负责传入本次处理所需的数据。
    @Override
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public List<MenuVO> currentTree() {
        // 准备actor，类型为 null；初始内容是调用 SecurityUtils.current 完成当前处理。
        var actor = SecurityUtils.current();
        // 检查条件 actor.mustChangePassword() 是否成立；成立时执行下面的分支。
        if (actor.mustChangePassword()) {
            // 把一个空列表，用来保存后续多条数据交还给调用方，本方法到此结束。
            return List.of();
        }
        // 准备全部（all），类型为 List<MenuEntity>；初始内容是从数据库读取符合条件的记录，具体 SQL 对应 findAll。
        List<MenuEntity> all = mapper.findAll();
        // 准备assigned，类型为 Set<Long>；初始内容是把前面筛选、转换后的结果汇成列表。
        Set<Long> assigned =
                // 调用 actor.superAdmin 完成superAdmin处理。
                actor.superAdmin()
                        // 继续补充同一个判断或备用结果：条件 ? all.stream().map(MenuEntity::getId).collect(Collectors.toSet()) 是否成立。
                        ? all.stream().map(MenuEntity::getId).collect(Collectors.toSet())
                        // 继续补充同一个判断或备用结果：条件 : new HashSet<>(mapper.findGrantedIds(actor.userId())); 是否成立。
                        : new HashSet<>(mapper.findGrantedIds(actor.userId()));
        // 准备权限Codes（permissionCodes），类型为 Map<Long, String>；初始内容是新建 摘要值对应表（HashMap） 实例。
        Map<Long, String> permissionCodes = new HashMap<>();
        // 权限（permission），使用 PermissionEntity 类型保存。
        // 逐次执行循环：for (PermissionEntity permission : permissions.findAll())；每轮处理一条数据或推进一次检查。
        for (PermissionEntity permission : permissions.findAll()) {
            // 检查当前用户是否具有需要的权限：Boolean.TRUE.equals(permission.getEnabled())；成立时执行下面的分支。
            if (Boolean.TRUE.equals(permission.getEnabled())) {
                // 保存指定键对应的值。
                permissionCodes.put(permission.getId(), permission.getCode());
            }
        }
        // 准备allowed，类型为 List<MenuEntity>；初始内容是逐项处理集合中的数据，继续做筛选、转换或汇总。
        List<MenuEntity> allowed =
                // 逐项处理集合中的数据，继续做筛选、转换或汇总。
                all.stream()
                        // 继续对上一步结果执行只保留符合条件的条目。
                        .filter(
                                // 当前记录（row），使用 null 类型保存。
                                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                                row ->
                                        // 判断集合是否包含指定内容。
                                        assigned.contains(row.getId())
                                                // 继续补充同一个判断或备用结果：条件 && Boolean.TRUE.equals(row.getEnabled()) 是否成立。
                                                && Boolean.TRUE.equals(row.getEnabled())
                                                // 继续补充同一个判断或备用结果：条件 && Boolean.TRUE.equals(row.getVisible())) 是否成立。
                                                && Boolean.TRUE.equals(row.getVisible()))
                        // 继续对上一步结果执行只保留符合条件的条目。
                        .filter(
                                // 当前记录（row），使用 null 类型保存。
                                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                                row ->
                                        // 继续传入或组合读取 权限编号（PermissionId） 的值，完成上面尚未结束的调用或声明。
                                        row.getPermissionId() == null
                                                // 继续补充同一个判断或备用结果：条件 || actor.superAdmin() 是否成立。
                                                || actor.superAdmin()
                                                // 继续补充同一个判断或备用结果：当前用户是否具有需要的权限：|| actor.permissions()。
                                                || actor.permissions()
                                                        // 继续对上一步结果执行判断集合是否包含指定内容。
                                                        .contains(
                                                                // 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的调用或声明。
                                                                permissionCodes.get(
                                                                        // 读取 权限编号（PermissionId） 的值。
                                                                        row.getPermissionId())))
                        // 继续对上一步结果执行调用 toList 完成to列表处理。
                        .toList();
        // 检查条件 com.asterway.common.utils.GrowthAccessUtils.manager(actor.roles()) 是否成立；成立时执行下面的分支。
        if (com.asterway.common.utils.GrowthAccessUtils.manager(actor.roles())) {
            // 把逐项处理集合中的数据，继续做筛选、转换或汇总保存到 allowed。
            allowed =
                    // 逐项处理集合中的数据，继续做筛选、转换或汇总。
                    allowed.stream()
                            // 继续对上一步结果执行只保留符合条件的条目。
                            .filter(
                                    // 当前记录（row），使用 null 类型保存。
                                    // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                                    row ->
                                            // 继续传入或组合调用 !com.asterway.common.utils.GrowthAccessUtils.menu 完成菜单处理，完成上面尚未结束的调用或声明。
                                            !com.asterway.common.utils.GrowthAccessUtils.menu(
                                                            // 读取 编码（Code） 的值。
                                                            row.getCode(), row.getComponentKey())
                                                    // 继续补充同一个判断或备用结果：条件 && !com.asterway.common.utils.GrowthAccessUtils 是否成立。
                                                    && !com.asterway.common.utils.GrowthAccessUtils
                                                            // 继续对上一步结果执行调用 permission 完成权限处理。
                                                            .permission(
                                                                    // 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的调用或声明。
                                                                    permissionCodes.get(
                                                                            // 读取 权限编号（PermissionId） 的值。
                                                                            row.getPermissionId())))
                            // 继续对上一步结果执行调用 toList 完成to列表处理。
                            .toList();
        }
        // 把组装 Tree交还给调用方，本方法到此结束。
        return buildTree(allowed);
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    @Override
    // 编号（id），使用 long 类型保存。
    public MenuVO get(long id) {
        // 要求满足 SuperAdmin。
        SecurityUtils.requireSuperAdmin("identity:menu:read");
        // 把把数据库记录转换为可以返回页面的对象交还给调用方，本方法到此结束。
        return toVO(required(id));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 创建（create）方法：调用 create 完成创建处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 请求参数对象（dto），使用 MenuSaveDTO 类型保存。
    public MenuVO create(MenuSaveDTO dto) {
        // 准备actor，类型为 null；初始内容是要求满足 SuperAdmin。
        var actor = SecurityUtils.requireSuperAdmin("identity:menu:create");
        // 调用 roles.lockSecurityGate 完成加锁SecurityGate处理。
        roles.lockSecurityGate();
        // 准备当前记录（row），类型为 MenuEntity；初始内容是新建 菜单（MenuEntity） 实例。
        MenuEntity row = new MenuEntity();
        // 检查 And填充（AndFill）。
        validateAndFill(row, dto);
        // 设置 创建时按（CreatedBy） 的值。
        row.setCreatedBy(actor.userId());
        // 设置 更新时按（UpdatedBy） 的值。
        row.setUpdatedBy(actor.userId());
        // 向数据库插入新记录，具体 SQL 对应 insert。
        mapper.insert(row);
        // 把新条目放到列表末尾。
        logs.append(
                // 继续传入或组合读取 编号（Id） 的值，完成上面尚未结束的调用或声明。
                "identity:menu:create", "MENU", row.getId().toString(), null, Map.of("version", 0));
        // 把读取 编号（Id） 的值交还给调用方，本方法到此结束。
        return toVO(required(row.getId()));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 MenuSaveDTO 类型保存。
    public MenuVO update(long id, MenuSaveDTO dto) {
        // 准备actor，类型为 null；初始内容是要求满足 SuperAdmin。
        var actor = SecurityUtils.requireSuperAdmin("identity:menu:update");
        // 调用 roles.lockSecurityGate 完成加锁SecurityGate处理。
        roles.lockSecurityGate();
        // 准备当前记录（row），类型为 MenuEntity；初始内容是读取必须存在的记录，不存在时给出业务错误。
        MenuEntity row = required(id);
        // 调用 AccountValidationUtils.version 完成版本号处理。
        AccountValidationUtils.version(dto.version(), row.getVersion());
        // 检查条件 Boolean.TRUE.equals(row.getBuiltin()) && !row.getCode().equals(dto.code()) 是否成立；成立时执行下面的分支。
        if (Boolean.TRUE.equals(row.getBuiltin()) && !row.getCode().equals(dto.code())) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("内置菜单编码不能修改");
        }
        // 检查 And填充（AndFill）。
        validateAndFill(row, dto);
        // 设置 更新时按（UpdatedBy） 的值。
        row.setUpdatedBy(actor.userId());
        // 检查条件 mapper.update(row) != 1 是否成立；成立时执行下面的分支。
        if (mapper.update(row) != 1) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("菜单已被修改，请刷新");
        }
        // 把新条目放到列表末尾。
        logs.append(
                // 继续传入或组合固定文字 'identity:menu:update'，完成上面尚未结束的调用或声明。
                "identity:menu:update",
                // 继续传入或组合固定状态或编码 MENU（菜单），完成上面尚未结束的调用或声明。
                "MENU",
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
    // 删除（delete）方法：调用 delete 完成删除处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    public void delete(long id, long version) {
        // 准备actor，类型为 null；初始内容是要求满足 SuperAdmin。
        var actor = SecurityUtils.requireSuperAdmin("identity:menu:delete");
        // 调用 roles.lockSecurityGate 完成加锁SecurityGate处理。
        roles.lockSecurityGate();
        // 准备当前记录（row），类型为 MenuEntity；初始内容是读取必须存在的记录，不存在时给出业务错误。
        MenuEntity row = required(id);
        // 调用 AccountValidationUtils.version 完成版本号处理。
        AccountValidationUtils.version(version, row.getVersion());
        // 检查条件 Boolean.TRUE.equals(row.getBuiltin()) || mapper.countReferences(id) > 0 是否成立；成立时执行下面的分支。
        if (Boolean.TRUE.equals(row.getBuiltin()) || mapper.countReferences(id) > 0) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("内置菜单、已分配菜单或仍有子节点的菜单不能删除");
        }
        // 设置 更新时按（UpdatedBy） 的值。
        row.setUpdatedBy(actor.userId());
        // 检查条件 mapper.softDelete(row) != 1 是否成立；成立时执行下面的分支。
        if (mapper.softDelete(row) != 1) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("菜单已被修改，请刷新");
        }
        // 把新条目放到列表末尾。
        logs.append(
                // 继续传入或组合固定文字 'identity:menu:delete'，完成上面尚未结束的调用或声明。
                "identity:menu:delete",
                // 继续传入或组合固定状态或编码 MENU（菜单），完成上面尚未结束的调用或声明。
                "MENU",
                // 调用 Long.toString 完成to文字处理。
                Long.toString(id),
                // 继续传入或组合空值，表示当前还没有对应内容，完成上面尚未结束的调用或声明。
                null,
                // 调用 Map.of 完成of处理。
                Map.of("version", version));
    }

    // 必填（required）方法：读取必须存在的记录，不存在时给出业务错误；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    private MenuEntity required(long id) {
        // 准备当前记录（row），类型为 MenuEntity；初始内容是从数据库读取符合条件的记录，具体 SQL 对应 findById。
        MenuEntity row = mapper.findById(id);
        // 检查row == null，即对应内容是否不存在；成立时执行下面的分支。
        if (row == null) {
            // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
            throw BusinessException.notFound();
        }
        // 把当前记录（row）交还给调用方，本方法到此结束。
        return row;
    }

    // 检查合法性And填充（validateAndFill）方法：检查 And填充（AndFill）；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 MenuEntity 类型保存。
    // 请求参数对象（dto），使用 MenuSaveDTO 类型保存。
    private void validateAndFill(MenuEntity row, MenuSaveDTO dto) {
        // 检查dto.name() == null || dto.name().isBlank() || dto.code() == null || !dto.code().matches("^[a-z][a-z0…，即对应内容是否不存在；成立时执行下面的分支。
        if (dto.name() == null
                // 继续补充同一个判断或备用结果：输入或集合是否为空：|| dto.name().isBlank()。
                || dto.name().isBlank()
                // 继续补充同一个判断或备用结果：|| dto.code() == null，即对应内容是否不存在。
                || dto.code() == null
                // 继续补充同一个判断或备用结果：条件 || !dto.code().matches("^[a-z][a-z0-9_]{1,63}$") 是否成立。
                || !dto.code().matches("^[a-z][a-z0-9_]{1,63}$")
                // 继续补充同一个判断或备用结果：|| dto.sortOrder() == null，即对应内容是否不存在。
                || dto.sortOrder() == null
                // 继续补充同一个判断或备用结果：条件 || dto.sortOrder() < 0 是否成立。
                || dto.sortOrder() < 0
                // 继续补充同一个判断或备用结果：|| dto.visible() == null，即对应内容是否不存在。
                || dto.visible() == null
                // 继续补充同一个判断或备用结果：|| dto.enabled() == null) {，即对应内容是否不存在。
                || dto.enabled() == null) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("菜单基本字段无效");
        }
        // 准备类型（type），类型为 String；初始内容是调用 dto.menuType 完成菜单类型处理。
        String type = dto.menuType();
        // 检查条件 !List.of("DIRECTORY", "PAGE", "BUTTON").contains(type) 是否成立；成立时执行下面的分支。
        if (!List.of("DIRECTORY", "PAGE", "BUTTON").contains(type)) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("菜单类型无效");
        }
        // 检查条件 "BUTTON".equals(type) 是否成立；成立时执行下面的分支。
        if ("BUTTON".equals(type)) {
            // 检查当前用户是否具有需要的权限：dto.routePath() != null || dto.componentKey() != null || dto.permissionId() == null || dto.parentId() == null；成立时执行下面的分支。
            if (dto.routePath() != null
                    // 继续补充同一个判断或备用结果：条件 || dto.componentKey() != null 是否成立。
                    || dto.componentKey() != null
                    // 继续补充同一个判断或备用结果：当前用户是否具有需要的权限：|| dto.permissionId() == null。
                    || dto.permissionId() == null
                    // 继续补充同一个判断或备用结果：|| dto.parentId() == null) {，即对应内容是否不存在。
                    || dto.parentId() == null) {
                // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
                throw BusinessException.badRequest("按钮必须属于页面并关联权限，不设置路由和组件");
            }
        // 继续传入或组合表达式 } else { 的结果，完成上面尚未结束的调用或声明。
        } else {
            // 检查dto.routePath() == null || !dto.routePath().matches("/[A-Za-z0-9/_-]*")，即对应内容是否不存在；成立时执行下面的分支。
            if (dto.routePath() == null || !dto.routePath().matches("/[A-Za-z0-9/_-]*")) {
                // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
                throw BusinessException.badRequest("菜单路径无效");
            }
            // 检查当前用户是否具有需要的权限："PAGE".equals(type) && (dto.componentKey() == null || !COMPONENTS.contains(dto.componentKey()) || dto.permissionId() == null)；成立时执行下面的分支。
            if ("PAGE".equals(type)
                    // 继续补充同一个判断或备用结果：&& (dto.componentKey() == null，即对应内容是否不存在。
                    && (dto.componentKey() == null
                            // 继续补充同一个判断或备用结果：条件 || !COMPONENTS.contains(dto.componentKey()) 是否成立。
                            || !COMPONENTS.contains(dto.componentKey())
                            // 继续补充同一个判断或备用结果：当前用户是否具有需要的权限：|| dto.permissionId() == null)) {。
                            || dto.permissionId() == null)) {
                // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
                throw BusinessException.badRequest("页面必须关联权限并使用有效组件键");
            }
            // 检查当前用户是否具有需要的权限："DIRECTORY".equals(type) && (dto.componentKey() != null || dto.permissionId() != null)；成立时执行下面的分支。
            if ("DIRECTORY".equals(type)
                    // 继续补充同一个判断或备用结果：当前用户是否具有需要的权限：&& (dto.componentKey() != null || dto.permissionId() != null)) {。
                    && (dto.componentKey() != null || dto.permissionId() != null)) {
                // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
                throw BusinessException.badRequest("目录不设置页面组件或动作权限");
            }
        }
        // 检查当前用户是否具有需要的权限：dto.permissionId() != null；成立时执行下面的分支。
        if (dto.permissionId() != null) {
            // 准备权限（permission），类型为 PermissionEntity；初始内容是查找 按编号（ById）。
            PermissionEntity permission = permissions.findById(dto.permissionId());
            // 检查当前用户是否具有需要的权限：permission == null || !Boolean.TRUE.equals(permission.getEnabled())；成立时执行下面的分支。
            if (permission == null || !Boolean.TRUE.equals(permission.getEnabled())) {
                // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
                throw BusinessException.badRequest("关联权限不存在或已停用");
            }
        }
        // 检查条件 row.getId() != null && !type.equals(row.getMenuType()) 是否成立；成立时执行下面的分支。
        if (row.getId() != null && !type.equals(row.getMenuType())) {
            // 下级（child），使用 MenuEntity 类型保存。
            // 逐次执行循环：for (MenuEntity child : mapper.findAll())；每轮处理一条数据或推进一次检查。
            for (MenuEntity child : mapper.findAll()) {
                // 检查条件 !row.getId().equals(child.getParentId()) 是否成立；成立时执行下面的分支。
                if (!row.getId().equals(child.getParentId())) {
                    // 继续传入或组合continue，完成上面尚未结束的调用或声明。
                    continue;
                }
                // 准备compatible，类型为 boolean；初始内容是优先使用前面的有效值，没有时采用后面的备用值："DIRECTORY".equals(type) && !"BUTTON".equals(child.getMenuType()) || "PAGE".equals(type) && "BUTTON".equals(ch…。
                boolean compatible =
                        // 继续传入或组合比较两个值是否相同，完成上面尚未结束的调用或声明。
                        "DIRECTORY".equals(type) && !"BUTTON".equals(child.getMenuType())
                                // 继续补充同一个判断或备用结果：条件 || "PAGE".equals(type) && "BUTTON".equals(child.getMenuType()); 是否成立。
                                || "PAGE".equals(type) && "BUTTON".equals(child.getMenuType());
                // 检查条件 !compatible 是否成立；成立时执行下面的分支。
                if (!compatible) {
                    // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
                    throw BusinessException.conflict("已有子节点与新菜单类型不兼容，请先调整子节点");
                }
            }
        }
        // 准备已读（seen），类型为 Set<Long>；初始内容是新建 摘要值设置（HashSet） 实例。
        Set<Long> seen = new HashSet<>();
        // 准备上级编号（parentId），类型为 Long；初始内容是调用 dto.parentId 完成上级编号处理。
        Long parentId = dto.parentId();
        // 检查条件 row.getId() != null 是否成立；成立时执行下面的分支。
        if (row.getId() != null) {
            // 加入集合或登记一条待保存记录。
            seen.add(row.getId());
        }
        // 准备immediate，类型为 boolean；初始内容是真，表示条件成立或开关打开。
        boolean immediate = true;
        // 逐次执行循环：while (parentId != null)；每轮处理一条数据或推进一次检查。
        while (parentId != null) {
            // 检查条件 !seen.add(parentId) 是否成立；成立时执行下面的分支。
            if (!seen.add(parentId)) {
                // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
                throw BusinessException.badRequest("菜单父链不能形成循环");
            }
            // 准备上级（parent），类型为 MenuEntity；初始内容是从数据库读取符合条件的记录，具体 SQL 对应 findById。
            MenuEntity parent = mapper.findById(parentId);
            // 检查parent == null || "BUTTON".equals(parent.getMenuType())，即对应内容是否不存在；成立时执行下面的分支。
            if (parent == null || "BUTTON".equals(parent.getMenuType())) {
                // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
                throw BusinessException.badRequest("父菜单不存在或不能拥有子节点");
            }
            // 检查条件 immediate && !("BUTTON".equals(type) ? "PAGE" : "DIRECTORY").equals(parent.getMenuType()) 是否成立；成立时执行下面的分支。
            if (immediate
                    // 继续补充同一个判断或备用结果：条件 && !("BUTTON".equals(type) ? "PAGE" : "DIRECTORY") 是否成立。
                    && !("BUTTON".equals(type) ? "PAGE" : "DIRECTORY")
                            // 继续对上一步结果执行读取 菜单类型（MenuType） 的值。
                            .equals(parent.getMenuType())) {
                // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
                throw BusinessException.badRequest("父节点类型不匹配");
            }
            // 把假，表示条件不成立或开关关闭保存到 immediate。
            immediate = false;
            // 把读取 上级编号（ParentId） 的值保存到 parentId。
            parentId = parent.getParentId();
        }
        // 设置 编码（Code） 的值。
        row.setCode(dto.code());
        // 设置 上级编号（ParentId） 的值。
        row.setParentId(dto.parentId());
        // 设置 名称（Name） 的值。
        row.setName(dto.name());
        // 设置 菜单类型（MenuType） 的值。
        row.setMenuType(type);
        // 设置 Route路径（RoutePath） 的值。
        row.setRoutePath(dto.routePath());
        // 设置 Component键（ComponentKey） 的值。
        row.setComponentKey(dto.componentKey());
        // 设置 权限编号（PermissionId） 的值。
        row.setPermissionId(dto.permissionId());
        // 设置 Icon 的值。
        row.setIcon(dto.icon());
        // 设置 排序顺序（SortOrder） 的值。
        row.setSortOrder(dto.sortOrder());
        // 设置 是否显示（Visible） 的值。
        row.setVisible(dto.visible());
        // 设置 是否启用（Enabled） 的值。
        row.setEnabled(dto.enabled());
    }

    // 构造Tree（buildTree）方法：组装 Tree；参数负责传入本次处理所需的数据。
    // 记录列表（rows），使用 List<MenuEntity> 类型保存。
    private List<MenuVO> buildTree(List<MenuEntity> rows) {
        // 准备按编号（byId），类型为 Map<Long, MenuEntity>；初始内容是新建 Linked摘要值对应表（LinkedHashMap） 实例。
        Map<Long, MenuEntity> byId = new LinkedHashMap<>();
        // 当前记录（row），使用 null 类型保存。
        // 调用 rows.forEach 完成forEach处理。
        // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
        rows.forEach(row -> byId.put(row.getId(), row));
        // 准备accepted，类型为 Map<Long, MenuVO>；初始内容是新建 Linked摘要值对应表（LinkedHashMap） 实例。
        Map<Long, MenuVO> accepted = new LinkedHashMap<>();
        // 当前记录（row），使用 MenuEntity 类型保存。
        // 逐次执行循环：for (MenuEntity row : rows)；每轮处理一条数据或推进一次检查。
        for (MenuEntity row : rows) {
            // 准备已读（seen），类型为 Set<Long>；初始内容是新建 摘要值设置（HashSet） 实例。
            Set<Long> seen = new HashSet<>();
            // 准备数据库语句执行器（cursor），类型为 MenuEntity；初始内容是当前记录（row）。
            MenuEntity cursor = row;
            // 准备有效（valid），类型为 boolean；初始内容是真，表示条件成立或开关打开。
            boolean valid = true;
            // 逐次执行循环：while (cursor != null)；每轮处理一条数据或推进一次检查。
            while (cursor != null) {
                // 检查条件 !seen.add(cursor.getId()) 是否成立；成立时执行下面的分支。
                if (!seen.add(cursor.getId())) {
                    // 把假，表示条件不成立或开关关闭保存到 valid。
                    valid = false;
                    // 继续传入或组合break，完成上面尚未结束的调用或声明。
                    break;
                }
                // 检查cursor.getParentId() == null，即对应内容是否不存在；成立时执行下面的分支。
                if (cursor.getParentId() == null) {
                    // 继续传入或组合break，完成上面尚未结束的调用或声明。
                    break;
                }
                // 把按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值保存到 cursor。
                cursor = byId.get(cursor.getParentId());
                // 检查cursor == null，即对应内容是否不存在；成立时执行下面的分支。
                if (cursor == null) {
                    // 把假，表示条件不成立或开关关闭保存到 valid。
                    valid = false;
                }
            }
            // 检查条件 valid 是否成立；成立时执行下面的分支。
            if (valid) {
                // 保存指定键对应的值。
                accepted.put(row.getId(), toVO(row));
            }
        }
        // 准备roots，类型为 List<MenuVO>；初始内容是一个空列表，用来保存后续多条数据。
        List<MenuVO> roots = new ArrayList<>();
        // 当前记录（row），使用 MenuEntity 类型保存。
        // 逐次执行循环：for (MenuEntity row : rows)；每轮处理一条数据或推进一次检查。
        for (MenuEntity row : rows) {
            // 准备节点（node），类型为 MenuVO；初始内容是按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            MenuVO node = accepted.get(row.getId());
            // 检查node == null，即对应内容是否不存在；成立时执行下面的分支。
            if (node == null) {
                // 继续传入或组合continue，完成上面尚未结束的调用或声明。
                continue;
            }
            // 检查row.getParentId() == null，即对应内容是否不存在；成立时执行下面的分支。
            if (row.getParentId() == null) {
                // 加入集合或登记一条待保存记录。
                roots.add(node);
            // 继续传入或组合表达式 } else { 的结果，完成上面尚未结束的调用或声明。
            } else {
                // 准备上级（parent），类型为 MenuVO；初始内容是按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
                MenuVO parent = accepted.get(row.getParentId());
                // 检查条件 parent != null 是否成立；成立时执行下面的分支。
                if (parent != null) {
                    // 调用 parent.children 完成下级列表处理。
                    parent.children().add(node);
                }
            }
        }
        // 把roots交还给调用方，本方法到此结束。
        return roots;
    }

    // 转换为页面需要的返回对象（toVO）方法：把数据库记录转换为可以返回页面的对象；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 MenuEntity 类型保存。
    private MenuVO toVO(MenuEntity row) {
        // 把新建 菜单（MenuVO） 实例交还给调用方，本方法到此结束。
        return new MenuVO(
                // 读取 编号（Id） 的值。
                row.getId().toString(),
                // 读取 编码（Code） 的值。
                row.getCode(),
                // 根据前面的条件，从两个候选结果中选择一个：row.getParentId() == null ? null : row.getParentId().toString()。
                row.getParentId() == null ? null : row.getParentId().toString(),
                // 读取 名称（Name） 的值。
                row.getName(),
                // 读取 菜单类型（MenuType） 的值。
                row.getMenuType(),
                // 读取 Route路径（RoutePath） 的值。
                row.getRoutePath(),
                // 读取 Component键（ComponentKey） 的值。
                row.getComponentKey(),
                // 根据前面的条件，从两个候选结果中选择一个：row.getPermissionId() == null ? null : row.getPermissionId().toString()。
                row.getPermissionId() == null ? null : row.getPermissionId().toString(),
                // 读取 Icon 的值。
                row.getIcon(),
                // 读取 排序顺序（SortOrder） 的值。
                row.getSortOrder(),
                // 比较两个值是否相同。
                Boolean.TRUE.equals(row.getVisible()),
                // 比较两个值是否相同。
                Boolean.TRUE.equals(row.getEnabled()),
                // 比较两个值是否相同。
                Boolean.TRUE.equals(row.getBuiltin()),
                // 读取 版本号（Version） 的值。
                row.getVersion(),
                // 继续传入或组合新建 Array列表（ArrayList） 实例，完成上面尚未结束的调用或声明。
                new ArrayList<>());
    }
}
