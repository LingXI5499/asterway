package com.asterway.identity.service;

import com.asterway.common.exception.BusinessException;
import com.asterway.identity.api.AccountApi;
import com.asterway.identity.api.PasswordHasher;
import com.asterway.identity.api.error.IdentityErrorCode;
import com.asterway.identity.api.model.AuthenticatedAccount;
import com.asterway.identity.api.model.LoginCommand;
import com.asterway.identity.api.model.RegisterCommand;
import com.asterway.identity.api.model.RegistrationResult;
import com.asterway.identity.mapper.AccountMapper;
import com.asterway.identity.model.AccountEntity;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Locale;
import java.util.Objects;

// 账户业务核心实现。
// 同时负责：注册、密码哈希、用户名查重、登录凭据验证。
// 它实现了 AccountApi 接口，是 identity 模块对外的实际业务入口。
@Service
public class AccountService implements AccountApi {

    // 账户数据访问（SQL 在 XML 里）
    private final AccountMapper accountMapper;

    // 密码哈希器（具体算法由 app 模块注入，这里是 Argon2）
    private final PasswordHasher passwordHasher;

    // 事务模板，用于把查重 + 插入 + 回读放到同一个数据库事务里
    private final TransactionTemplate transactionTemplate;

    public AccountService(
            AccountMapper accountMapper,
            PasswordHasher passwordHasher,
            PlatformTransactionManager transactionManager
    ) {
        this.accountMapper = accountMapper;
        this.passwordHasher = passwordHasher;
        this.transactionTemplate =
                new TransactionTemplate(transactionManager);
    }

    @Override
    public RegistrationResult register(RegisterCommand command) {

        // 第一步：校验两次输入的密码是否一致
        if (!Objects.equals(
                command.password(),
                command.confirmPassword()
        )) {
            throw new BusinessException(
                    IdentityErrorCode.PASSWORD_MISMATCH
            );
        }

        // 用户名规范化（转小写），用于和数据库生成的 username_key 比较
        String username = command.username();
        String usernameKey = normalizeUsername(username);

        // 昵称为空时回退为用户名
        String nickname = command.nickname();
        if (nickname == null || nickname.isBlank()) {
            nickname = username;
        }

        // 在数据库事务外执行耗时的密码哈希计算，避免占用数据库事务
        String passwordHash = passwordHasher.hash(
                command.password()
        );

        String finalNickname = nickname;

        // 用事务模板执行核心写操作：查重 -> 插入 -> 回读数据库生成的字段
        return Objects.requireNonNull(
                transactionTemplate.execute(transactionStatus -> {

                    // 第一次查重：只判断是否存在，给用户友好的业务错误
                    if (accountMapper.existsByUsernameKey(usernameKey)) {
                        throw new BusinessException(
                                IdentityErrorCode.USERNAME_TAKEN
                        );
                    }

                    // 构造待插入的实体；id、username_key、时间字段由数据库生成
                    AccountEntity account = new AccountEntity(
                            null,          // id 由数据库生成
                            username,
                            null,          // username_key 由数据库生成
                            finalNickname,
                            passwordHash,
                            "USER",        // 新账户默认角色
                            "ENABLED",     // 新账户默认启用
                            null,          // created_at 由数据库生成
                            null           // updated_at 由数据库生成
                    );

                    try {
                        int rows = accountMapper.insertAccount(account);

                        if (rows != 1) {
                            throw new IllegalStateException(
                                    "账户插入影响行数异常"
                            );
                        }
                    } catch (DataIntegrityViolationException exception) {
                        // 并发注册时，先查重可能没查到，但插入时撞上唯一约束。
                        // 这里识别是否是用户名唯一约束冲突，是的话转成业务错误。
                        if (isUsernameUniqueViolation(exception)) {
                            throw new BusinessException(
                                    IdentityErrorCode.USERNAME_TAKEN,
                                    exception
                            );
                        }
                        // 其他完整性错误原样抛出
                        throw exception;
                    }

                    // 重新读一次，拿到数据库生成的 id 和创建时间
                    AccountEntity saved =
                            accountMapper.findByUsernameKey(usernameKey);

                    if (saved == null) {
                        throw new IllegalStateException(
                                "账户插入后未能读取账户记录"
                        );
                    }

                    // 组装注册结果返回
                    return new RegistrationResult(
                            saved.id(),
                            saved.username(),
                            true,
                            saved.createdAt().toInstant()
                    );
                })
        );
    }

    @Override
    public AuthenticatedAccount authenticate(LoginCommand command) {

        String usernameKey = normalizeUsername(command.username());

        // 按规范化用户名查询账户（含密码哈希，仅内部使用）
        AccountEntity account =
                accountMapper.findByUsernameKey(usernameKey);

        // 不区分"用户名不存在 / 账户不可用 / 密码错误"这三种情况，
        // 统一返回"凭据无效"，避免攻击者通过错误信息枚举出有效账户。
        if (account == null
                || !"ENABLED".equals(account.status())
                || !passwordHasher.matches(
                command.password(),
                account.passwordHash()
        )) {
            throw new BusinessException(
                    IdentityErrorCode.CREDENTIALS_INVALID
            );
        }

        // 返回安全账户摘要（不含密码哈希）
        return new AuthenticatedAccount(
                account.id(),
                account.username(),
                account.nickname(),
                account.role()
        );
    }

    // 用户名规范化：统一转小写，保证大小写不敏感的唯一性比较
    private String normalizeUsername(String username) {
        return username.toLowerCase(Locale.ROOT);
    }

    // 判断异常链里是否包含"用户名唯一约束"冲突。
    // 通过约束名称 uq_app_user_username_key 识别，避免把其他完整性错误误判成用户名重复。
    private boolean isUsernameUniqueViolation(Throwable exception) {

        Throwable current = exception;

        while (current != null) {
            String message = current.getMessage();

            if (message != null
                    && message.contains("uq_app_user_username_key")) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}
