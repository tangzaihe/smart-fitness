package com.aseantec.smartfitness.infra.mybatis;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

/**
 * 把 {@link OffsetDateTime} 以 JDBC {@code TIMESTAMP WITH TIME ZONE} 绑定，避免当成 varchar。
 */
@MappedTypes(OffsetDateTime.class)
@MappedJdbcTypes({JdbcType.TIMESTAMP, JdbcType.TIMESTAMP_WITH_TIMEZONE, JdbcType.OTHER})
public class OffsetDateTimeTypeHandler extends BaseTypeHandler<OffsetDateTime> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, OffsetDateTime parameter, JdbcType jdbcType) throws SQLException {
        ps.setObject(i, parameter);
    }

    @Override
    public OffsetDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getObject(columnName, OffsetDateTime.class);
    }

    @Override
    public OffsetDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getObject(columnIndex, OffsetDateTime.class);
    }

    @Override
    public OffsetDateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return cs.getObject(columnIndex, OffsetDateTime.class);
    }
}
