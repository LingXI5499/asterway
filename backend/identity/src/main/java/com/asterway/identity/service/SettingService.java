/*
 * 规定Setting业务服务可供其他代码调用的业务方法。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.service;

// 引入Setting更新（SettingUpdateDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.SettingUpdateDTO;
// 引入PublicSetting（PublicSettingVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.PublicSettingVO;
// 引入Setting（SettingVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.SettingVO;

/* 两项固定账户设置，不提供任意配置项增删接口。 */
// 定义接口 SettingService；规定Setting业务服务可供其他代码调用的业务方法。
public interface SettingService {

    /* 超管读取配置及版本。 */
    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    SettingVO get();

    /* 公开读取站点名称和注册开关，不返回配置元数据。 */
    // public配置（publicSettings）方法：调用 publicSettings 完成public配置处理；参数负责传入本次处理所需的数据。
    PublicSettingVO publicSettings();

    /* 在注册调用方事务内共享锁定开关，关闭时拒绝写入。 */
    // requireRegistration是否展开（requireRegistrationOpen）方法：要求满足 Registration是否展开（RegistrationOpen）；参数负责传入本次处理所需的数据。
    void requireRegistrationOpen();

    /* 同一事务比较两项旧版本并更新，失败整体回滚。 */
    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    // 请求参数对象（dto），使用 SettingUpdateDTO 类型保存。
    SettingVO update(SettingUpdateDTO dto);
}
