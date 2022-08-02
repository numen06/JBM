package com.jbm.framework.masterdata.typehandler;

import com.jbm.framework.masterdata.usage.DicEnum;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;

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
public class DicEnumTypeHandler extends BaseTypeHandler<DicEnum> {
    private Class<DicEnum> type;

    public DicEnumTypeHandler() {
        super();
    }

    public DicEnumTypeHandler(Class<DicEnum> type) {
        if (type == null) {
            throw new IllegalArgumentException("Type argument cannot be null");
        }
        this.type = type;
    }


    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, DicEnum parameter, JdbcType jdbcType) throws SQLException {
        ps.setNString(i, parameter.getKey().toString());
    }

    @Override
    public DicEnum getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return convert(rs.getNString(columnName));
    }

    @Override
    public DicEnum getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return convert(rs.getNString(columnIndex));
    }

    @Override
    public DicEnum getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return convert(cs.getNString(columnIndex));
    }

    private DicEnum convert(String key) {
        DicEnum[] objs = type.getEnumConstants();
        for (DicEnum em : objs) {
            if (em.getKey().toString().equals(key)) {
                return em;
            }
        }
        return null;
    }

}
