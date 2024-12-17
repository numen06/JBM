package jbm.framework.boot.autoconfigure.td;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.db.*;
import cn.hutool.db.dialect.impl.AnsiSqlDialect;
import cn.hutool.db.sql.SqlBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author wesley
 */
public class TdSqlDialect  extends AnsiSqlDialect {

    public PreparedStatement psForInsert(Connection conn, Entity entity) throws SQLException {
        SqlBuilder insert = SqlBuilder.create(this.wrapper).insert(entity, this.dialectName());
        return StatementUtil.prepareStatement(false, conn, insert.build(), insert.getParamValueArray());
    }

}
