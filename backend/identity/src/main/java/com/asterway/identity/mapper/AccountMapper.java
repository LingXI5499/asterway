package com.asterway.identity.mapper;

import com.asterway.identity.model.AccountState;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// 账户主体和数据库时间的数据访问契约。
public interface AccountMapper {

    OffsetDateTime now();

    void lockScope(@Param("scope") String scope);

    AccountState findState(@Param("userId") UUID userId);

    AccountState findByLoginName(@Param("usernameKey") String usernameKey);

    int insertState(AccountState account);

    int updateProfile(
            @Param("userId") UUID userId,
            @Param("username") String username,
            @Param("nickname") String nickname,
            @Param("renamed") boolean renamed
    );

    int updatePassword(@Param("userId") UUID userId, @Param("passwordHash") String passwordHash);

    List<AccountState> listSecurityStates();
}
