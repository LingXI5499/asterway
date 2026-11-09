/*
 * 提供Setting请求入口的 HTTP 入口，校验输入后交给业务服务处理。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.controller;

// 引入结果（ResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.ResultVO;
// 引入Setting更新（SettingUpdateDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.SettingUpdateDTO;
// 引入Setting业务服务（SettingService），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.service.SettingService;
// 引入PublicSetting（PublicSettingVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.PublicSettingVO;
// 引入Setting（SettingVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.SettingVO;
// 引入有效（Valid），下面可以直接使用这个类型或静态成员。
import jakarta.validation.Valid;
// 引入提前Authorize（PreAuthorize），下面可以直接使用这个类型或静态成员。
import org.springframework.security.access.prepost.PreAuthorize;
// 引入Validated，下面可以直接使用这个类型或静态成员。
import org.springframework.validation.annotation.Validated;
// 引入读取字段对应关系（GetMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.GetMapping;
// 引入Put字段对应关系（PutMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.PutMapping;
// 引入请求请求正文（RequestBody），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RequestBody;
// 引入请求字段对应关系（RequestMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RequestMapping;
// 引入Rest请求入口（RestController），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.RestController;

/* 两项账户配置的读取与更新入口。 */
// 把这个类作为 HTTP 接口入口，并把返回对象编码成 JSON。
// 定义类 SettingController；提供Setting请求入口的 HTTP 入口，校验输入后交给业务服务处理。
@RestController
// 启用方法参数上的校验规则。
@Validated
// 设置这组接口共同使用的地址前缀或请求条件。
@RequestMapping("/asterway")
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class SettingController {

    // 业务服务（service），使用 SettingService 类型保存。
    private final SettingService service;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 业务服务（service），使用 SettingService 类型保存。
    public SettingController(SettingService service) {
        // 把业务服务（service）保存到 this.service。
        this.service = service;
    }

    /* 读取公开配置。 */
    // 把 GET 请求交给下面的方法，用来读取数据。
    // public配置（publicSettings）方法：调用 publicSettings 完成public配置处理；参数负责传入本次处理所需的数据。
    @GetMapping("/public/account-settings")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<PublicSettingVO> publicSettings() {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.publicSettings());
    }

    /* 超管读取完整配置及版本。 */
    // 把 GET 请求交给下面的方法，用来读取数据。
    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    @GetMapping("/settings/account")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:setting:read')")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<SettingVO> get() {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.get());
    }

    /* 超管原子更新两项配置。 */
    // 把 PUT 请求交给下面的方法，用来整体更新资源。
    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    @PutMapping("/settings/account")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:setting:update')")
    // 继续检查这个对象内部字段上的校验规则。
    // 从请求 JSON 正文中读取这个参数对象。
    // 请求参数对象（dto），使用 SettingUpdateDTO 类型保存。
    public ResultVO<SettingVO> update(@Valid @RequestBody SettingUpdateDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.update(dto));
    }
}
