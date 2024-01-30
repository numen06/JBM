package com.jbm.util.db.load;

import cn.hutool.core.util.StrUtil;
import com.jbm.util.SimpleTemplateUtils;
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
    public String renderSql(String sqlName, String fileContent, Object... params) {
        try {
            if (params.length == 1) {
                return SimpleTemplateUtils.renderStringTemplate(fileContent, params[0]);
            }
            return SimpleTemplateUtils.renderStringTemplate(fileContent, params);
        } catch (Exception e) {
            log.error("转换sql失败", e);
        }
        return null;
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
