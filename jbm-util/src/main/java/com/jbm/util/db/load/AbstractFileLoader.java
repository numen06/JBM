package com.jbm.util.db.load;

import cn.hutool.core.io.resource.ResourceUtil;
import com.jbm.util.db.sqltemplate.SqlMeta;

public abstract class AbstractFileLoader implements FileLoader {

    private final String sqlPath;

    public AbstractFileLoader(String sqlPath) {
        this.sqlPath = sqlPath;
    }

    public String loadFile(String sqlName) {
        String fileName = buildFileName(sqlName, getExtension());
        return ResourceUtil.readUtf8Str(fileName);
    }

    public abstract SqlMeta renderSql(String sqlName, String fileContent, Object... params);

    public abstract String getExtension();

    /**
     * @param sqlName
     * @return
     */
    @Override
    public SqlMeta load(String sqlName, Object... params) {
        String fileContent = this.loadFile(sqlName);
        return this.renderSql(sqlName, fileContent, params);
    }

    protected String buildFileName(String fileName, String extName) {
        return sqlPath + fileName + extName;
    }

}
