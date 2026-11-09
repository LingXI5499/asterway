/*
 * 声明Setting数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.mapper;

// 引入Setting（SettingEntity），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.entity.SettingEntity;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;
// 引入访问数据库的 Mapper（Mapper），下面可以直接使用这个类型或静态成员。
import org.apache.ibatis.annotations.Mapper;

/* Setting 数据访问接口，同一表的查询和修改集中在同名 XML。 */
// 让 MyBatis 为这个数据库访问接口创建实现。
// 定义接口 SettingMapper；声明Setting数据库读写器数据库操作，方法实际执行的 SQL 位于同名 Mapper XML。
@Mapper
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public interface SettingMapper {

    /* 读取账户配置。 */
    // 查找全部（findAll）方法：查找 全部（All）；参数负责传入本次处理所需的数据。
    List<SettingEntity> findAll();

    /* 注册写入时共享锁定开关，和关闭注册事务协调。 */
    // 加锁Registration（lockRegistration）方法：调用 lockRegistration 完成加锁Registration处理；参数负责传入本次处理所需的数据。
    SettingEntity lockRegistration();

    /* 按版本修改固定配置键。 */
    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    // entity，使用 SettingEntity 类型保存。
    int update(SettingEntity entity);
}
