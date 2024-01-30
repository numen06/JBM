package com.jbm.util.db.load;

public interface FileLoader {

    String load(String sqlName, Object... params);

    boolean canRead(String sqlName);
}
