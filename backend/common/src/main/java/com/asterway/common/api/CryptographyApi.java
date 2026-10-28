package com.asterway.common.api;



// 安全材料计算契约；密钥配置和文件实现由应用层持有。
public interface CryptographyApi {

    byte[] digest(String purpose, Object value);

    String digestHex(String purpose, Object value);

    byte[] encrypt(Object value);

    <T> T decrypt(byte[] encrypted, Class<T> type);
}
