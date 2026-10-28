package com.asterway.identity.mapper;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

// PostgreSQL UUID 与 Java UUID 之间的类型处理器。
// MyBatis 3.5.19 没有内置 UUID 类型处理器，而本项目主键用 PostgreSQL 的 uuid 类型，
// 所以需要自己注册，否则会报 "No typehandler found for property" 的映射错误。
@MappedTypes(UUID.class)
public class UuidTypeHandler extends BaseTypeHandler<UUID> {

    // 写入：把 UUID 作为 JDBC 的 OTHER 类型交给 PostgreSQL 驱动处理
    @Override
    public void setNonNullParameter(
            PreparedStatement ps,
            int i,
            UUID parameter,
            JdbcType jdbcType
    ) throws SQLException {
        ps.setObject(i, parameter, Types.OTHER);
    }

    // 读取：按列名取 UUID
    @Override
    public UUID getNullableResult(
            ResultSet rs,
            String columnName
    ) throws SQLException {
        return toUuid(rs.getObject(columnName));
    }

    // 读取：按列下标取 UUID
    @Override
    public UUID getNullableResult(
            ResultSet rs,
            int columnIndex
    ) throws SQLException {
        return toUuid(rs.getObject(columnIndex));
    }

    // 读取：从存储过程结果里按列下标取 UUID
    @Override
    public UUID getNullableResult(
            CallableStatement cs,
            int columnIndex
    ) throws SQLException {
        return toUuid(cs.getObject(columnIndex));
    }

    // 把 JDBC 返回的对象安全转成 UUID；兼容驱动返回 String 或 UUID 两种情况
    private UUID toUuid(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(value.toString());
    }
}
