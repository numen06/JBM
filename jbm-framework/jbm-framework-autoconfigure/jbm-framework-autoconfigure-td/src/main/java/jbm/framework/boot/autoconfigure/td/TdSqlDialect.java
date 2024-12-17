package jbm.framework.boot.autoconfigure.td;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.*;
import cn.hutool.db.dialect.impl.AnsiSqlDialect;
import cn.hutool.db.sql.SqlBuilder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author wesley
 */
@Slf4j
public class TdSqlDialect  extends AnsiSqlDialect {

    public PreparedStatement psForInsert(Connection conn, Entity entity) throws SQLException {
        SqlBuilder insert = SqlBuilder.create(this.wrapper).insert(entity, this.dialectName());
        try {

            return StatementUtil.prepareStatement(false, conn, insert.build(), insert.getParamValueArray());
        } catch (SQLException e) {
            String sql = insert.build();
            // 逐个替换占位符
            //替换第一个占位符，防止替换多次导致索引错乱
            sql = StrUtil.replace(sql, "?", "{}");
            sql = StrUtil.format(sql, insert.getParamValueArray());
            log.error("TDSQL方言插入数据异常:{}", sql, e);
            throw e;
        }

    }

}
