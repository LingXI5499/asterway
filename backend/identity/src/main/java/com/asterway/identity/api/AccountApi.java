package com.asterway.identity.api;

import com.asterway.identity.api.model.AuthenticatedAccount;
import com.asterway.identity.api.model.LoginCommand;
import com.asterway.identity.api.model.RegisterCommand;
import com.asterway.identity.api.model.RegistrationResult;

// 账户模块对外公开的业务契约（模块间通信的入口）。
// 这是 identity 模块暴露给上层（app 模块）的唯一业务接口，上层只依赖它，
// 不关心内部实现（密码算法、数据存哪张表），从而实现模块解耦。
// 目前只有两个核心能力：register 注册、authenticate 校验登录凭据。
public interface AccountApi {

    // 注册一个新账户
    // command：注册命令（用户名、昵称、密码、确认密码）
    // 返回：注册结果（含数据库生成的账户 ID、创建时间等）
    RegistrationResult register(RegisterCommand command);

    // 校验登录凭据
    // command：登录命令（用户名、密码）
    // 返回：校验通过后的安全账户摘要（不含密码哈希）
    AuthenticatedAccount authenticate(LoginCommand command);
}
