/*
 * 实现权限业务服务业务规则，处理权限、状态、数据读写和必要的事务。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.service.impl;

// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入账号ValidationUtils（AccountValidationUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.AccountValidationUtils;
// 引入SecurityUtils，下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.SecurityUtils;
// 引入权限查询条件（PermissionQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.PermissionQueryDTO;
// 引入权限保存（PermissionSaveDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.PermissionSaveDTO;
// 引入权限（PermissionEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.entity.PermissionEntity;
// 引入权限数据库读写器（PermissionMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.mapper.PermissionMapper;
// 引入角色数据库读写器（RoleMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.mapper.RoleMapper;
// 引入操作日志业务服务（OperationLogService），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.service.OperationLogService;
// 引入权限业务服务（PermissionService），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.service.PermissionService;
// 引入权限（PermissionVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.PermissionVO;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;
// 引入Locale，下面可以直接使用这个类型或静态成员。
import java.util.Locale;
// 引入按键保存值的对应表（Map），下面可以直接使用这个类型或静态成员。
import java.util.Map;
// 引入业务服务（Service），下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Service;
// 引入Transactional，下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.annotation.Transactional;

/* 权限的查询、保存与删除放在同一服务，所有写操作与授权变更共用安全门锁。 */
// 把该实现交给 Spring 管理，其他组件可以注入并调用。
// 定义类 PermissionServiceImpl；实现权限业务服务业务规则，处理权限、状态、数据读写和必要的事务。
@Service
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class PermissionServiceImpl implements PermissionService {

    // 访问数据库的 Mapper（mapper），使用 PermissionMapper 类型保存。
    private final PermissionMapper mapper;

    // 角色列表（roles），使用 RoleMapper 类型保存。
    private final RoleMapper roles;

    // logs，使用 OperationLogService 类型保存。
    private final OperationLogService logs;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    public PermissionServiceImpl(
            // 访问数据库的 Mapper（mapper），使用 PermissionMapper 类型保存。
            PermissionMapper mapper,
            // 角色列表（roles），使用 RoleMapper 类型保存。
            RoleMapper roles,
            // logs，使用 OperationLogService 类型保存。
            OperationLogService logs) {
        // 把访问数据库的 Mapper（mapper）保存到 this.mapper。
        this.mapper = mapper;
        // 把角色列表（roles）保存到 this.roles。
        this.roles = roles;
        // 把logs保存到 this.logs。
        this.logs = logs;
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    @Override
    // 查询条件（query），使用 PermissionQueryDTO 类型保存。
    public PageResultVO<PermissionVO> page(PermissionQueryDTO query) {
        // 要求满足 SuperAdmin。
        SecurityUtils.requireSuperAdmin("identity:permission:list");
        // 准备分页时跳过的记录数（offset），类型为 int；初始内容是调用 query.offset 完成分页时跳过的记录数处理。
        int offset = query.offset();
        // 准备keyword，类型为 String；初始内容是根据前面的条件，从两个候选结果中选择一个：query.getKeyword() == null ? "" : query.getKeyword().toLowerCase(Locale.ROOT)。
        String keyword = query.getKeyword() == null ? "" : query.getKeyword().toLowerCase(Locale.ROOT);
        // 准备matches，类型为 List<PermissionEntity>；初始内容是逐项处理集合中的数据，继续做筛选、转换或汇总。
        List<PermissionEntity> matches = mapper.findAll().stream()
                // 当前记录（row），使用 null 类型保存。
                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                .filter(row -> query.getEnabled() == null || query.getEnabled().equals(row.getEnabled()))
                // 当前记录（row），使用 null 类型保存。
                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                .filter(row -> row.getCode().toLowerCase(Locale.ROOT).contains(keyword)
                        // 继续补充同一个判断或备用结果：条件 || row.getName().toLowerCase(Locale.ROOT).contains(keyword)) 是否成立。
                        || row.getName().toLowerCase(Locale.ROOT).contains(keyword))
                // 继续对上一步结果执行调用 toList 完成to列表处理。
                .toList();
        // 把新建 当前页结果（PageResultVO） 实例交还给调用方，本方法到此结束。
        return new PageResultVO<>(matches.stream().skip(offset).limit(query.getSize()).map(this::toVO).toList(),
                // 调用 matches.size 完成每页条数处理。
                matches.size(), query.getPage(), query.getSize());
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    @Override
    // 编号（id），使用 long 类型保存。
    public PermissionVO get(long id) {
        // 要求满足 SuperAdmin。
        SecurityUtils.requireSuperAdmin("identity:permission:read");
        // 把把数据库记录转换为可以返回页面的对象交还给调用方，本方法到此结束。
        return toVO(required(id));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 创建（create）方法：调用 create 完成创建处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 请求参数对象（dto），使用 PermissionSaveDTO 类型保存。
    public PermissionVO create(PermissionSaveDTO dto) {
        // 准备actor，类型为 null；初始内容是要求满足 SuperAdmin。
        var actor = SecurityUtils.requireSuperAdmin("identity:permission:create");
        // 调用 roles.lockSecurityGate 完成加锁SecurityGate处理。
        roles.lockSecurityGate();
        // 准备当前记录（row），类型为 PermissionEntity；初始内容是新建 权限（PermissionEntity） 实例。
        PermissionEntity row = new PermissionEntity();
        // 把已校验的请求字段填入待保存的记录。
        fill(row, dto);
        // 设置 创建时按（CreatedBy） 的值。
        row.setCreatedBy(actor.userId());
        // 设置 更新时按（UpdatedBy） 的值。
        row.setUpdatedBy(actor.userId());
        // 向数据库插入新记录，具体 SQL 对应 insert。
        mapper.insert(row);
        // 把新条目放到列表末尾。
        logs.append("identity:permission:create", "PERMISSION", row.getId().toString(), null, Map.of("version", 0));
        // 把读取 编号（Id） 的值交还给调用方，本方法到此结束。
        return toVO(required(row.getId()));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 PermissionSaveDTO 类型保存。
    public PermissionVO update(long id, PermissionSaveDTO dto) {
        // 准备actor，类型为 null；初始内容是要求满足 SuperAdmin。
        var actor = SecurityUtils.requireSuperAdmin("identity:permission:update");
        // 调用 roles.lockSecurityGate 完成加锁SecurityGate处理。
        roles.lockSecurityGate();
        // 准备当前记录（row），类型为 PermissionEntity；初始内容是读取必须存在的记录，不存在时给出业务错误。
        PermissionEntity row = required(id);
        // 调用 AccountValidationUtils.version 完成版本号处理。
        AccountValidationUtils.version(dto.version(), row.getVersion());
        // 检查条件 Boolean.TRUE.equals(row.getBuiltin()) && !row.getCode().equals(dto.code()) 是否成立；成立时执行下面的分支。
        if (Boolean.TRUE.equals(row.getBuiltin()) && !row.getCode().equals(dto.code())) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("内置权限编码不能修改");
        }
        // 把已校验的请求字段填入待保存的记录。
        fill(row, dto);
        // 设置 更新时按（UpdatedBy） 的值。
        row.setUpdatedBy(actor.userId());
        // 检查条件 mapper.update(row) != 1 是否成立；成立时执行下面的分支。
        if (mapper.update(row) != 1) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("权限已被修改，请刷新");
        }
        // 把新条目放到列表末尾。
        logs.append("identity:permission:update", "PERMISSION", Long.toString(id), null, Map.of("version", dto.version()));
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
        var actor = SecurityUtils.requireSuperAdmin("identity:permission:delete");
        // 调用 roles.lockSecurityGate 完成加锁SecurityGate处理。
        roles.lockSecurityGate();
        // 准备当前记录（row），类型为 PermissionEntity；初始内容是读取必须存在的记录，不存在时给出业务错误。
        PermissionEntity row = required(id);
        // 调用 AccountValidationUtils.version 完成版本号处理。
        AccountValidationUtils.version(version, row.getVersion());
        // 检查条件 Boolean.TRUE.equals(row.getBuiltin()) || mapper.countReferences(id) > 0 是否成立；成立时执行下面的分支。
        if (Boolean.TRUE.equals(row.getBuiltin()) || mapper.countReferences(id) > 0) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("内置权限或仍被引用的权限不能删除");
        }
        // 设置 更新时按（UpdatedBy） 的值。
        row.setUpdatedBy(actor.userId());
        // 检查条件 mapper.softDelete(row) != 1 是否成立；成立时执行下面的分支。
        if (mapper.softDelete(row) != 1) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("权限已被修改，请刷新");
        }
        // 把新条目放到列表末尾。
        logs.append("identity:permission:delete", "PERMISSION", Long.toString(id), null, Map.of("version", version));
    }

    // 必填（required）方法：读取必须存在的记录，不存在时给出业务错误；参数负责传入本次处理所需的数据。
    // 编号（id），使用 long 类型保存。
    private PermissionEntity required(long id) {
        // 准备当前记录（row），类型为 PermissionEntity；初始内容是从数据库读取符合条件的记录，具体 SQL 对应 findById。
        PermissionEntity row = mapper.findById(id);
        // 检查row == null，即对应内容是否不存在；成立时执行下面的分支。
        if (row == null) {
            // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
            throw BusinessException.notFound();
        }
        // 把当前记录（row）交还给调用方，本方法到此结束。
        return row;
    }

    // 填充（fill）方法：把已校验的请求字段填入待保存的记录；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 PermissionEntity 类型保存。
    // 请求参数对象（dto），使用 PermissionSaveDTO 类型保存。
    private void fill(PermissionEntity row, PermissionSaveDTO dto) {
        // 检查dto.code() == null || !dto.code().matches("^[a-z][a-z0-9_]*(:[a-z][a-z0-9_]*){2,3}$") || dto.name() …，即对应内容是否不存在；成立时执行下面的分支。
        if (dto.code() == null || !dto.code().matches("^[a-z][a-z0-9_]*(:[a-z][a-z0-9_]*){2,3}$")
                // 继续补充同一个判断或备用结果：|| dto.name() == null || dto.name().isBlank() || dto.enabled() == null) {，即对应内容是否不存在。
                || dto.name() == null || dto.name().isBlank() || dto.enabled() == null) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("权限字段无效");
        }
        // 设置 编码（Code） 的值。
        row.setCode(dto.code());
        // 设置 名称（Name） 的值。
        row.setName(dto.name());
        // 设置 说明（Description） 的值。
        row.setDescription(dto.description() == null ? "" : dto.description());
        // 设置 是否启用（Enabled） 的值。
        row.setEnabled(dto.enabled());
    }

    // 转换为页面需要的返回对象（toVO）方法：把数据库记录转换为可以返回页面的对象；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 PermissionEntity 类型保存。
    private PermissionVO toVO(PermissionEntity row) {
        // 把新建 权限（PermissionVO） 实例交还给调用方，本方法到此结束。
        return new PermissionVO(row.getId().toString(), row.getCode(), row.getName(), row.getDescription(),
                // 比较两个值是否相同。
                Boolean.TRUE.equals(row.getBuiltin()), Boolean.TRUE.equals(row.getEnabled()), row.getVersion());
    }

}
