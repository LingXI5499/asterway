/*
 * 提供用户请求入口的 HTTP 入口，校验输入后交给业务服务处理。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.controller;

// 引入当前页查询条件（PageQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.dto.PageQueryDTO;
// 引入Captcha结果（CaptchaResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.CaptchaResultVO;
// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;
// 引入结果（ResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.ResultVO;
// 引入Captcha创建（CaptchaCreateDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.CaptchaCreateDTO;
// 引入Captcha校验（CaptchaVerifyDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.CaptchaVerifyDTO;
// 引入登录（LoginDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.LoginDTO;
// 引入密码Reset（PasswordResetDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.PasswordResetDTO;
// 引入密码更新（PasswordUpdateDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.PasswordUpdateDTO;
// 引入Registration（RegistrationDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.RegistrationDTO;
// 引入时间时区更新（TimeZoneUpdateDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.TimeZoneUpdateDTO;
// 引入用户创建（UserCreateDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.UserCreateDTO;
// 引入用户查询条件（UserQueryDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.UserQueryDTO;
// 引入用户角色列表（UserRolesDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.UserRolesDTO;
// 引入用户状态（UserStatusDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.UserStatusDTO;
// 引入用户更新（UserUpdateDTO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.dto.UserUpdateDTO;
// 引入用户业务服务（UserService），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.service.UserService;
// 引入Captcha本次请求序号（CaptchaTicketVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.CaptchaTicketVO;
// 引入当前用户（CurrentUserVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.CurrentUserVO;
// 引入设备（DeviceVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.DeviceVO;
// 引入登录结果（LoginResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.LoginResultVO;
// 引入时间时区（TimeZoneVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.TimeZoneVO;
// 引入用户统计（UserStatisticsVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.UserStatisticsVO;
// 引入用户（UserVO），下面可以直接使用这个类型或静态成员。
import com.asterway.identity.vo.UserVO;
// 引入有效（Valid），下面可以直接使用这个类型或静态成员。
import jakarta.validation.Valid;
// 引入正数（Positive），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Positive;
// 引入非负数校验规则（PositiveOrZero），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.PositiveOrZero;
// 引入长度或数量校验规则（Size），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Size;
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
// 引入模型Attribute（ModelAttribute），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.ModelAttribute;
// 引入Patch字段对应关系（PatchMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.PatchMapping;
// 引入路径Variable（PathVariable），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.PathVariable;
// 引入Post字段对应关系（PostMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.PostMapping;
// 引入Put字段对应关系（PutMapping），下面可以直接使用这个类型或静态成员。
import org.springframework.web.bind.annotation.PutMapping;
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

/* 用户主链的 HTTP 入口；Controller 只校验接入参数并调用 Service 接口。 */
// 把这个类作为 HTTP 接口入口，并把返回对象编码成 JSON。
// 定义类 UserController；提供用户请求入口的 HTTP 入口，校验输入后交给业务服务处理。
@RestController
// 启用方法参数上的校验规则。
@Validated
// 设置这组接口共同使用的地址前缀或请求条件。
@RequestMapping("/asterway")
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class UserController {

    // 业务服务（service），使用 UserService 类型保存。
    private final UserService service;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // 业务服务（service），使用 UserService 类型保存。
    public UserController(UserService service) {
        // 把业务服务（service）保存到 this.service。
        this.service = service;
    }

    /* 创建场景绑定拼图。 */
    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // 创建Captcha（createCaptcha）方法：创建 Captcha；参数负责传入本次处理所需的数据。
    @PostMapping("/captchas")
    // 继续检查这个对象内部字段上的校验规则。
    // 从请求 JSON 正文中读取这个参数对象。
    // 请求参数对象（dto），使用 CaptchaCreateDTO 类型保存。
    public ResultVO<CaptchaResultVO> createCaptcha(@Valid @RequestBody CaptchaCreateDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.createCaptcha(dto));
    }

    /* 验证拼图并取得一次性凭据。 */
    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // 校验Captcha（verifyCaptcha）方法：调用 verifyCaptcha 完成校验Captcha处理；参数负责传入本次处理所需的数据。
    @PostMapping("/captcha-verifications")
    // 继续检查这个对象内部字段上的校验规则。
    // 从请求 JSON 正文中读取这个参数对象。
    // 请求参数对象（dto），使用 CaptchaVerifyDTO 类型保存。
    public ResultVO<CaptchaTicketVO> verifyCaptcha(@Valid @RequestBody CaptchaVerifyDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.verifyCaptcha(dto));
    }

    /* 公开注册普通用户，成功后仍需登录。 */
    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // register方法：调用 register 完成register处理；参数负责传入本次处理所需的数据。
    @PostMapping("/registrations")
    // 指定此方法成功时使用的 HTTP 状态码。
    @ResponseStatus(HttpStatus.CREATED)
    // 继续检查这个对象内部字段上的校验规则。
    // 从请求 JSON 正文中读取这个参数对象。
    // 请求参数对象（dto），使用 RegistrationDTO 类型保存。
    public ResultVO<UserVO> register(@Valid @RequestBody RegistrationDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.register(dto));
    }

    /* 创建独立登录会话。 */
    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // 登录（login）方法：调用 login 完成登录处理；参数负责传入本次处理所需的数据。
    @PostMapping("/sessions")
    // 指定此方法成功时使用的 HTTP 状态码。
    @ResponseStatus(HttpStatus.CREATED)
    // 继续检查这个对象内部字段上的校验规则。
    // 从请求 JSON 正文中读取这个参数对象。
    // 请求参数对象（dto），使用 LoginDTO 类型保存。
    public ResultVO<LoginResultVO> login(@Valid @RequestBody LoginDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.login(dto));
    }

    /* 读取当前账户与授权菜单。 */
    // 把 GET 请求交给下面的方法，用来读取数据。
    // 当前登录用户（me）方法：调用 me 完成当前登录用户处理；参数负责传入本次处理所需的数据。
    @GetMapping("/me")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.hasPermission('identity:profile:read')")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<CurrentUserVO> me() {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.me());
    }

    /* 修改本人用户名或昵称。 */
    // 把 PATCH 请求交给下面的方法，用来修改部分字段。
    // 更新Me（updateMe）方法：更新 当前登录用户（Me）；参数负责传入本次处理所需的数据。
    @PatchMapping("/me")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.hasPermission('identity:profile:update')")
    // 继续检查这个对象内部字段上的校验规则。
    // 从请求 JSON 正文中读取这个参数对象。
    // 请求参数对象（dto），使用 UserUpdateDTO 类型保存。
    public ResultVO<UserVO> updateMe(@Valid @RequestBody UserUpdateDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.updateMe(dto));
    }

    /* 校验原密码后修改密码，成功退出全部旧设备。 */
    // 把 PUT 请求交给下面的方法，用来整体更新资源。
    // 修改密码（changePassword）方法：调用 changePassword 完成修改密码处理；参数负责传入本次处理所需的数据。
    @PutMapping("/me/password")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.hasPermission('identity:password:update')")
    // 继续检查这个对象内部字段上的校验规则。
    // 从请求 JSON 正文中读取这个参数对象。
    // 请求参数对象（dto），使用 PasswordUpdateDTO 类型保存。
    public ResultVO<Void> changePassword(@Valid @RequestBody PasswordUpdateDTO dto) {
        // 调用 service.changePassword 完成修改密码处理。
        service.changePassword(dto);
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(null);
    }

    /* 读取本人时区状态。 */
    // 把 GET 请求交给下面的方法，用来读取数据。
    // 用户使用的时区（timeZone）方法：调用 timeZone 完成用户使用的时区处理；参数负责传入本次处理所需的数据。
    @GetMapping("/me/time-zone")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.hasPermission('identity:timezone:read')")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<TimeZoneVO> timeZone() {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.timeZone());
    }

    /* 保存设备时区，按旧时区日边界生效。 */
    // 把 PUT 请求交给下面的方法，用来整体更新资源。
    // 更新时间时区（updateTimeZone）方法：更新 时间时区（TimeZone）；参数负责传入本次处理所需的数据。
    @PutMapping("/me/time-zone")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.hasPermission('identity:timezone:update')")
    // 继续检查这个对象内部字段上的校验规则。
    // 从请求 JSON 正文中读取这个参数对象。
    // 请求参数对象（dto），使用 TimeZoneUpdateDTO 类型保存。
    public ResultVO<TimeZoneVO> updateTimeZone(@Valid @RequestBody TimeZoneUpdateDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.updateTimeZone(dto));
    }

    /* 查询可用 IANA 时区。 */
    // 把 GET 请求交给下面的方法，用来读取数据。
    // 时间Zones（timeZones）方法：调用 timeZones 完成时间Zones处理；参数负责传入本次处理所需的数据。
    @GetMapping("/time-zones")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<PageResultVO<String>> timeZones(
            // 从 URL 查询参数中读取这个值。
            // 限制集合条数或文字长度，边界以注解中的数值为准。
            // keyword，使用 String 类型保存；可为 null，表示尚未提供。
            @RequestParam(required = false) @Size(max = 128) String keyword,
            // 继续检查这个对象内部字段上的校验规则。
            // 把查询参数按名称装入这个筛选或分页对象。
            // 查询条件（query），使用 PageQueryDTO 类型保存。
            @Valid @ModelAttribute PageQueryDTO query
    ) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.timeZones(keyword, query));
    }

    /* 分页查询管理范围内用户。 */
    // 把 GET 请求交给下面的方法，用来读取数据。
    // 当前页（page）方法：调用 page 完成当前页处理；参数负责传入本次处理所需的数据。
    @GetMapping("/users")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.hasPermission('identity:user:list')")
    // 继续检查这个对象内部字段上的校验规则。
    // 把查询参数按名称装入这个筛选或分页对象。
    // 查询条件（query），使用 UserQueryDTO 类型保存。
    public ResultVO<PageResultVO<UserVO>> page(@Valid @ModelAttribute UserQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.page(query));
    }

    /* 查询管理范围内账户数量。 */
    // 把 GET 请求交给下面的方法，用来读取数据。
    // 统计（statistics）方法：调用 statistics 完成统计处理；参数负责传入本次处理所需的数据。
    @GetMapping("/user-statistics")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.hasPermission('identity:user:count')")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<UserStatisticsVO> statistics() {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.statistics());
    }

    /* 查询管理范围内用户详情。 */
    // 把 GET 请求交给下面的方法，用来读取数据。
    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    @GetMapping("/users/{id}")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.hasPermission('identity:user:read')")
    // 从 URL 路径中读取这个参数，例如记录编号。
    // 要求数值大于零。
    // 编号（id），使用 long 类型保存。
    public ResultVO<UserVO> get(@PathVariable @Positive long id) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.get(id));
    }

    /* 新增普通账户，不允许直接指定管理身份。 */
    // 把 POST 请求交给下面的方法，用来创建资源或执行动作。
    // 创建（create）方法：调用 create 完成创建处理；参数负责传入本次处理所需的数据。
    @PostMapping("/users")
    // 指定此方法成功时使用的 HTTP 状态码。
    @ResponseStatus(HttpStatus.CREATED)
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.hasPermission('identity:user:create')")
    // 继续检查这个对象内部字段上的校验规则。
    // 从请求 JSON 正文中读取这个参数对象。
    // 请求参数对象（dto），使用 UserCreateDTO 类型保存。
    public ResultVO<UserVO> create(@Valid @RequestBody UserCreateDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.create(dto));
    }

    /* 修改同一用户资料。 */
    // 把 PATCH 请求交给下面的方法，用来修改部分字段。
    // 更新（update）方法：调用 update 完成更新处理；参数负责传入本次处理所需的数据。
    @PatchMapping("/users/{id}")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.hasPermission('identity:user:update')")
    // 从 URL 路径中读取这个参数，例如记录编号。
    // 要求数值大于零。
    // 继续检查这个对象内部字段上的校验规则。
    // 从请求 JSON 正文中读取这个参数对象。
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 UserUpdateDTO 类型保存。
    public ResultVO<UserVO> update(@PathVariable @Positive long id, @Valid @RequestBody UserUpdateDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.update(id, dto));
    }

    /* 逻辑删除账户，保留审计与历史身份。 */
    // 把 DELETE 请求交给下面的方法，具体删除规则由业务服务决定。
    // 删除（delete）方法：调用 delete 完成删除处理；参数负责传入本次处理所需的数据。
    @DeleteMapping("/users/{id}")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.hasPermission('identity:user:delete')")
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

    /* 启停账户。 */
    // 把 PATCH 请求交给下面的方法，用来修改部分字段。
    // 更新状态（updateStatus）方法：更新 状态（Status）；参数负责传入本次处理所需的数据。
    @PatchMapping("/users/{id}/status")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.hasPermission('identity:user:status')")
    // 从 URL 路径中读取这个参数，例如记录编号。
    // 要求数值大于零。
    // 继续检查这个对象内部字段上的校验规则。
    // 从请求 JSON 正文中读取这个参数对象。
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 UserStatusDTO 类型保存。
    public ResultVO<UserVO> updateStatus(@PathVariable @Positive long id, @Valid @RequestBody UserStatusDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.updateStatus(id, dto));
    }

    /* 重置密码并要求首次修改。 */
    // 把 PUT 请求交给下面的方法，用来整体更新资源。
    // reset密码（resetPassword）方法：调用 resetPassword 完成reset密码处理；参数负责传入本次处理所需的数据。
    @PutMapping("/users/{id}/password")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.hasPermission('identity:user:reset_password')")
    // 从 URL 路径中读取这个参数，例如记录编号。
    // 要求数值大于零。
    // 继续检查这个对象内部字段上的校验规则。
    // 从请求 JSON 正文中读取这个参数对象。
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 PasswordResetDTO 类型保存。
    public ResultVO<Void> resetPassword(@PathVariable @Positive long id, @Valid @RequestBody PasswordResetDTO dto) {
        // 调用 service.resetPassword 完成reset密码处理。
        service.resetPassword(id, dto);
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(null);
    }

    /* 超管读取目标角色回显。 */
    // 把 GET 请求交给下面的方法，用来读取数据。
    // 角色列表（roles）方法：调用 roles 完成角色列表处理；参数负责传入本次处理所需的数据。
    @GetMapping("/users/{id}/roles")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:user:assign_role')")
    // 从 URL 路径中读取这个参数，例如记录编号。
    // 要求数值大于零。
    // 编号（id），使用 long 类型保存。
    public ResultVO<List<String>> roles(@PathVariable @Positive long id) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.roles(id));
    }

    /* 超管授予或撤销 ADMIN 附加身份。 */
    // 把 PUT 请求交给下面的方法，用来整体更新资源。
    // 安排角色列表（assignRoles）方法：调用 assignRoles 完成安排角色列表处理；参数负责传入本次处理所需的数据。
    @PutMapping("/users/{id}/roles")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.isSuperAdmin() and @access.hasPermission('identity:user:assign_role')")
    // 从 URL 路径中读取这个参数，例如记录编号。
    // 要求数值大于零。
    // 继续检查这个对象内部字段上的校验规则。
    // 从请求 JSON 正文中读取这个参数对象。
    // 编号（id），使用 long 类型保存。
    // 请求参数对象（dto），使用 UserRolesDTO 类型保存。
    public ResultVO<UserVO> assignRoles(@PathVariable @Positive long id, @Valid @RequestBody UserRolesDTO dto) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.assignRoles(id, dto));
    }

    /* 读取本人有效设备，不进行会话保活。 */
    // 把 GET 请求交给下面的方法，用来读取数据。
    // my会话列表（mySessions）方法：调用 mySessions 完成my会话列表处理；参数负责传入本次处理所需的数据。
    @GetMapping("/me/sessions")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.hasPermission('identity:device:list')")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<List<DeviceVO>> mySessions() {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.mySessions());
    }

    /* 退出当前设备。 */
    // 把 DELETE 请求交给下面的方法，具体删除规则由业务服务决定。
    // 退出登录（logout）方法：调用 logout 完成退出登录处理；参数负责传入本次处理所需的数据。
    @DeleteMapping("/me/sessions/current")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<Void> logout() {
        // 调用 service.logout 完成退出登录处理。
        service.logout();
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(null);
    }

    /* 退出本人指定设备。 */
    // 把 DELETE 请求交给下面的方法，具体删除规则由业务服务决定。
    // revokeMy会话（revokeMySession）方法：调用 revokeMySession 完成revokeMy会话处理；参数负责传入本次处理所需的数据。
    @DeleteMapping("/me/sessions/{id}")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.hasPermission('identity:device:revoke')")
    // 从 URL 路径中读取这个参数，例如记录编号。
    // 限制集合条数或文字长度，边界以注解中的数值为准。
    // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
    public ResultVO<Void> revokeMySession(@PathVariable @Size(max = 64) String id) {
        // 调用 service.revokeMySession 完成revokeMy会话处理。
        service.revokeMySession(id);
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(null);
    }

    /* 退出全部设备并提升数据库安全版本。 */
    // 把 DELETE 请求交给下面的方法，具体删除规则由业务服务决定。
    // 退出登录全部（logoutAll）方法：调用 logoutAll 完成退出登录全部处理；参数负责传入本次处理所需的数据。
    @DeleteMapping("/me/sessions")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.hasPermission('identity:device:revoke')")
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public ResultVO<Void> logoutAll() {
        // 调用 service.logoutAll 完成退出登录全部处理。
        service.logoutAll();
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(null);
    }

    /* 查询管理范围内的有效会话。 */
    // 把 GET 请求交给下面的方法，用来读取数据。
    // 会话列表（sessions）方法：调用 sessions 完成会话列表处理；参数负责传入本次处理所需的数据。
    @GetMapping("/sessions")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.hasPermission('identity:session:list')")
    // 继续检查这个对象内部字段上的校验规则。
    // 把查询参数按名称装入这个筛选或分页对象。
    // 查询条件（query），使用 PageQueryDTO 类型保存。
    public ResultVO<PageResultVO<DeviceVO>> sessions(@Valid @ModelAttribute PageQueryDTO query) {
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(service.sessions(query));
    }

    /* 强退管理范围内的指定会话。 */
    // 把 DELETE 请求交给下面的方法，具体删除规则由业务服务决定。
    // revoke会话（revokeSession）方法：调用 revokeSession 完成revoke会话处理；参数负责传入本次处理所需的数据。
    @DeleteMapping("/sessions/{id}")
    // 在方法执行前检查表达式中的权限，不通过就拒绝调用。
    @PreAuthorize("@access.hasPermission('identity:session:revoke')")
    // 从 URL 路径中读取这个参数，例如记录编号。
    // 限制集合条数或文字长度，边界以注解中的数值为准。
    // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
    public ResultVO<Void> revokeSession(@PathVariable @Size(max = 64) String id) {
        // 调用 service.revokeSession 完成revoke会话处理。
        service.revokeSession(id);
        // 把调用 ResultVO.success 完成成功处理交还给调用方，本方法到此结束。
        return ResultVO.success(null);
    }
}
