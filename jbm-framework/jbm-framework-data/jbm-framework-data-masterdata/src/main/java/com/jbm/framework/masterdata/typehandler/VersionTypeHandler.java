package com.jbm.framework.masterdata.typehandler;

import com.alibaba.fastjson.JSON;
import com.jbm.util.bean.Version;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import javax.persistence.AttributeConverter;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-03-03 04:09
 **/
@MappedJdbcTypes(value = JdbcType.VARCHAR, includeNullJdbcType = true)
@MappedTypes({Version.class})
@Slf4j
public class VersionTypeHandler extends BaseTypeHandler<Version> implements AttributeConverter<Object, String> {

    public VersionTypeHandler() {
        super();
    }

    @Override
    public String convertToDatabaseColumn(Object o) {
        return JSON.toJSONString(o);
    }

    @Override
    public Object convertToEntityAttribute(String s) {
        return JSON.parseObject(s);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Version parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.toString());
    }

    @Override
    public Version getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return convert(rs.getString(columnName));
    }

    @Override
    public Version getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return convert(rs.getString(columnIndex));
    }

    @Override
    public Version getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return convert(cs.getString(columnIndex));
    }

    private Version convert(String val) {
        return Version.parse(val);
    }

}
