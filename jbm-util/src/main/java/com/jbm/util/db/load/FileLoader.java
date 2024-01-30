package com.jbm.util.db.load;

import com.jbm.util.db.sqltemplate.SqlMeta;

public interface FileLoader {

    SqlMeta load(String sqlName, Object... params);

    boolean canRead(String sqlName);
}
