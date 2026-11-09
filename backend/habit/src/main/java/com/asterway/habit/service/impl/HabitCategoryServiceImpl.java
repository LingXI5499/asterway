/*
 * 实现习惯分类业务服务业务规则，处理权限、状态、数据读写和必要的事务。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.habit.service.impl;

// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入成长业务上下文Utils（GrowthContextUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.GrowthContextUtils;
// 引入习惯分类保存（HabitCategorySaveDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.dto.HabitCategorySaveDTO;
// 引入习惯查询条件（HabitQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.dto.HabitQueryDTO;
// 引入习惯分类（HabitCategoryEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.entity.HabitCategoryEntity;
// 引入习惯分类数据库读写器（HabitCategoryMapper），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.mapper.HabitCategoryMapper;
// 引入习惯分类业务服务（HabitCategoryService），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.service.HabitCategoryService;
// 引入习惯分类（HabitCategoryVO），下面可以直接使用这个类型或静态成员。
import com.asterway.habit.vo.HabitCategoryVO;

// 引入业务服务（Service），下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Service;
// 引入Isolation，下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.annotation.Isolation;
// 引入Transactional，下面可以直接使用这个类型或静态成员。
import org.springframework.transaction.annotation.Transactional;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* 分类删除只停止新选择，不级联修改习惯及其快照。 */
// 把该实现交给 Spring 管理，其他组件可以注入并调用。
// 定义类 HabitCategoryServiceImpl；实现习惯分类业务服务业务规则，处理权限、状态、数据读写和必要的事务。
@Service
// 把下面的修改放进事务，成功一起提交，异常按配置回滚。
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class HabitCategoryServiceImpl implements HabitCategoryService {
    // 访问数据库的 Mapper（mapper），使用 HabitCategoryMapper 类型保存。
    private final HabitCategoryMapper mapper;
    // 当前用户、权限和数据范围检查器（ctx），使用 GrowthContextUtils 类型保存。
    private final GrowthContextUtils ctx;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 访问数据库的 Mapper（mapper），使用 HabitCategoryMapper 类型保存。
    // 当前用户、权限和数据范围检查器（ctx），使用 GrowthContextUtils 类型保存。
    public HabitCategoryServiceImpl(HabitCategoryMapper mapper, GrowthContextUtils ctx) {
        // 把访问数据库的 Mapper（mapper）保存到 this.mapper。
        this.mapper = mapper;
        // 把当前用户、权限和数据范围检查器（ctx）保存到 this.ctx。
        this.ctx = ctx;
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    @Override
    // 查询条件（query），使用 HabitQueryDTO 类型保存。
    public PageResultVO<HabitCategoryVO> page(HabitQueryDTO query) {
        // 准备用户编号（userId），类型为 long；初始内容是核对读取权限，并取得当前普通用户编号。
        long userId = ctx.read("habit:category:list");
        // 调用 query.offset 完成分页时跳过的记录数处理。
        query.offset();
        // 检查条件 query.getKeyword() != null && query.getKeyword().length() > 100 是否成立；成立时执行下面的分支。
        if (query.getKeyword() != null && query.getKeyword().length() > 100) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("分类关键字过长");
        }
        // 把新建 当前页结果（PageResultVO） 实例交还给调用方，本方法到此结束。
        return new PageResultVO<>(
                // 把前面筛选、转换后的结果汇成列表。
                mapper.page(userId, query).stream().map(this::view).toList(),
                // 统计符合条件的记录数，具体 SQL 对应 count。
                mapper.count(userId, query),
                // 读取 当前页（Page） 的值。
                query.getPage(),
                // 读取 长度或数量校验规则（Size） 的值。
                query.getSize());
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 创建（create）方法：调用 create 完成创建处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 请求参数对象（dto），使用 HabitCategorySaveDTO 类型保存。
    public HabitCategoryVO create(HabitCategorySaveDTO dto) {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write("habit:category:create");
        // 准备当前记录（row），类型为 null；初始内容是新建 习惯分类（HabitCategoryEntity） 实例。
        var row = new HabitCategoryEntity();
        // 设置 用户编号（UserId） 的值。
        row.setUserId(userId);
        // 设置 名称（Name） 的值。
        row.setName(name(dto.name()));
        // 设置 排序顺序（SortOrder） 的值。
        row.setSortOrder(ctx.order(dto.sortOrder()));
        // 读取 名称（Name） 的值。
        unique(userId, row.getName(), null);
        // 设置 创建时At（CreatedAt） 的值。
        row.setCreatedAt(Instant.now());
        // 设置 更新时At（UpdatedAt） 的值。
        row.setUpdatedAt(row.getCreatedAt());
        // 核对数据库实际更新条数，失败时按冲突处理。
        ctx.changed(mapper.insert(row));
        // 把读取 编号（Id） 的值交还给调用方，本方法到此结束。
        return view(required(userId, row.getId()));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 HabitCategorySaveDTO 类型保存。
    public HabitCategoryVO update(long id, HabitCategorySaveDTO dto) {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write("habit:category:update");
        // 准备当前记录（row），类型为 null；初始内容是读取必须存在的记录，不存在时给出业务错误。
        var row = required(userId, id);
        // 比较数据库版本与请求版本，旧版本不能覆盖新数据。
        ctx.version(row.getVersion(), dto.version());
        // 准备名称（name），类型为 String；初始内容是调用 name(dto.name 完成名称处理。
        String name = name(dto.name());
        // 准备顺序（order），类型为 int；初始内容是调用 ctx.order 完成顺序处理。
        int order = ctx.order(dto.sortOrder());
        // 检查条件 row.getName().equals(name) && row.getSortOrder() == order 是否成立；成立时执行下面的分支。
        if (row.getName().equals(name) && row.getSortOrder() == order) {
            // 把调用 view 完成页面处理交还给调用方，本方法到此结束。
            return view(row);
        }
        // 调用 unique 完成唯一处理。
        unique(userId, name, id);
        // 设置 名称（Name） 的值。
        row.setName(name);
        // 设置 排序顺序（SortOrder） 的值。
        row.setSortOrder(order);
        // 设置 更新时At（UpdatedAt） 的值。
        row.setUpdatedAt(Instant.now());
        // 核对数据库实际更新条数，失败时按冲突处理。
        ctx.changed(mapper.update(row));
        // 把调用 view 完成页面处理交还给调用方，本方法到此结束。
        return view(required(userId, id));
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // 删除（delete）方法：调用 delete 完成删除处理；参数负责传入本次处理所需的数据。
    @Override
    // 把下面的修改放进事务，成功一起提交，异常按配置回滚。
    @Transactional
    // 编号（id），使用 long 类型保存。
    // 版本号（version），使用 long 类型保存。
    public void delete(long id, long version) {
        // 准备用户编号（userId），类型为 long；初始内容是核对写入权限，并锁定当前用户的业务操作。
        long userId = ctx.write("habit:category:delete");
        // 准备当前记录（row），类型为 null；初始内容是取得第一条符合条件的数据。
        var row = mapper.find(userId, id);
        // 检查row == null，即对应内容是否不存在；成立时执行下面的分支。
        if (row == null) {
            // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
            throw BusinessException.notFound();
        }
        // 检查条件 row.getDeletedAt() != null 是否成立；成立时执行下面的分支。
        if (row.getDeletedAt() != null) {
            // 把返回（return）交还给调用方，本方法到此结束。
            return;
        }
        // 比较数据库版本与请求版本，旧版本不能覆盖新数据。
        ctx.version(row.getVersion(), version);
        // 核对数据库实际更新条数，失败时按冲突处理。
        ctx.changed(mapper.softDelete(userId, id, version, Instant.now()));
    }

    // 名称（name）方法：调用 name 完成名称处理；参数负责传入本次处理所需的数据。
    // 值（value），使用 String 类型保存；可为 null，表示尚未提供。
    private String name(String value) {
        // 检查value == null || value.isBlank()，即对应内容是否不存在；成立时执行下面的分支。
        if (value == null || value.isBlank()) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("分类名称不能为空");
        }
        // 准备结果（result），类型为 String；初始内容是去掉文字两端的空白。
        String result = value.trim();
        // 检查条件 result.codePointCount(0, result.length()) > 50 是否成立；成立时执行下面的分支。
        if (result.codePointCount(0, result.length()) > 50) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("分类名称不能超过 50 个字符");
        }
        // 把结果（result）交还给调用方，本方法到此结束。
        return result;
    }

    // 唯一（unique）方法：调用 unique 完成唯一处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 名称（name），使用 String 类型保存；可为 null，表示尚未提供。
    // except编号（exceptId），使用 Long 类型保存；可为 null，表示尚未提供。
    private void unique(long userId, String name, Long exceptId) {
        // 检查条件 mapper.nameExists(userId, name, exceptId) 是否成立；成立时执行下面的分支。
        if (mapper.nameExists(userId, name, exceptId)) {
            // 中止正常处理并抛出异常：调用 BusinessException.conflict 完成conflict处理；由上层按失败处理。
            throw BusinessException.conflict("已存在同名有效分类");
        }
    }

    // 必填（required）方法：读取必须存在的记录，不存在时给出业务错误；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    // 编号（id），使用 long 类型保存。
    private HabitCategoryEntity required(long userId, long id) {
        // 准备当前记录（row），类型为 null；初始内容是取得第一条符合条件的数据。
        var row = mapper.find(userId, id);
        // 检查row == null || row.getDeletedAt() != null，即对应内容是否不存在；成立时执行下面的分支。
        if (row == null || row.getDeletedAt() != null) {
            // 中止正常处理并抛出异常：调用 BusinessException.notFound 完成notFound处理；由上层按失败处理。
            throw BusinessException.notFound();
        }
        // 把当前记录（row）交还给调用方，本方法到此结束。
        return row;
    }

    // 页面（view）方法：调用 view 完成页面处理；参数负责传入本次处理所需的数据。
    // 当前记录（row），使用 HabitCategoryEntity 类型保存。
    private HabitCategoryVO view(HabitCategoryEntity row) {
        // 把新建 习惯分类（HabitCategoryVO） 实例交还给调用方，本方法到此结束。
        return new HabitCategoryVO(
                // 读取 编号（Id） 的值。
                row.getId().toString(), row.getName(), row.getSortOrder(), row.getVersion());
    }
}
