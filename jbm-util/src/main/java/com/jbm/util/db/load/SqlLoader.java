package com.jbm.util.db.load;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.jbm.util.SimpleTemplateUtils;
import com.jbm.util.db.sqltemplate.SqlMeta;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wesley
 */
@Slf4j
public class SqlLoader extends AbstractFileLoader {

    public SqlLoader(String sqlPath) {
        super(sqlPath);
    }

    @Override
    public SqlMeta renderSql(String sqlName, String fileContent, Object... params) {
        SqlMeta sqlMeta = null;
        try {
            if (params.length == 1) {
                String sql = SimpleTemplateUtils.renderStringTemplate(fileContent, params[0]);
                int paramCount = StrUtil.count(sql, "?");
                sqlMeta = new SqlMeta(sql, CollUtil.newArrayList(params).subList(0, paramCount));
                return sqlMeta;
            }
            String sql = SimpleTemplateUtils.renderStringTemplate(fileContent, params);
            int paramCount = StrUtil.count(sql, "?");
            sqlMeta = new SqlMeta(sql, CollUtil.newArrayList(params).subList(0, paramCount));
        } catch (Exception e) {
            log.error("转换sql失败", e);
        }
        return sqlMeta;
    }

    @Override
    public String getExtension() {
        return ".sql";
    }

    @Override
    public boolean canRead(String sqlName) {
        return !StrUtil.contains(sqlName, ".");
    }
}
