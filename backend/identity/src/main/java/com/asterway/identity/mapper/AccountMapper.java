package com.asterway.identity.mapper;

import com.asterway.identity.model.AccountEntity;
import org.apache.ibatis.annotations.Param;

// 账户数据访问接口（MyBatis Mapper）。
// 这里只声明 SQL 方法签名，具体 SQL 写在 AccountMapper.xml 里（项目约定：SQL 统一放 XML）。
public interface AccountMapper {

    // 新增一条账户记录。
    // 注意：id、username_key、created_at、updated_at 都由数据库生成，插入时不传入。
    // account：待插入的账户实体
    // 返回：受影响行数（正常应为 1）
    int insertAccount(AccountEntity account);

    // 按规范化用户名查询账户。
    // 返回完整实体（含密码哈希），仅在登录校验时使用，不能返回给前端。
    // usernameKey：规范化后的用户名（小写）
    // 返回：账户实体，不存在时返回 null
    AccountEntity findByUsernameKey(
            @Param("usernameKey") String usernameKey
    );

    // 判断用户名是否已存在（只查存在性，不读密码哈希等敏感字段）。
    // usernameKey：规范化后的用户名（小写）
    // 返回：存在返回 true，否则 false
    boolean existsByUsernameKey(
            @Param("usernameKey") String usernameKey
    );
}
