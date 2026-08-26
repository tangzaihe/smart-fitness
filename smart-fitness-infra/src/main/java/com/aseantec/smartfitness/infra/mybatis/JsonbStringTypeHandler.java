package com.aseantec.smartfitness.infra.mybatis;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 把 Java {@link String} 以 PostgreSQL {@code jsonb} 写入，避免 varchar/jsonb 类型冲突。
 */
@MappedTypes(String.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class JsonbStringTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        PGobject jsonObject = new PGobject();
        jsonObject.setType("jsonb");
        jsonObject.setValue(parameter);
        ps.setObject(i, jsonObject);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toJson(rs.getObject(columnName));
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toJson(rs.getObject(columnIndex));
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toJson(cs.getObject(columnIndex));
    }

    private static String toJson(Object value) {
        return value == null ? null : value.toString();
    }
}
