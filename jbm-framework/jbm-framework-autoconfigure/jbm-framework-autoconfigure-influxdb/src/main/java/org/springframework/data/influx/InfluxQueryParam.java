package org.springframework.data.influx;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 传送查询条件
 *
 * @author wesley.zhang
 */
public class InfluxQueryParam implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -2100738311438780557L;

    /**
     * SQL查询参数
     */
    private Object params;

    /**
     * 查询db
     */
    private String database;

    /**
     * 补充列
     */
    private Map<String, Object> supplementColumns = new HashMap<>();

    public InfluxQueryParam() {
        super();
    }

    public InfluxQueryParam(String database, Object params) {
        super();
        this.params = params;
        this.database = database;
    }

    public Object getParams() {
        return params;
    }

    public void setParams(Object params) {
        this.params = params;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public Map<String, Object> getSupplementColumns() {
        return supplementColumns;
    }

    public void setSupplementColumns(Map<String, Object> supplementColumns) {
        this.supplementColumns = supplementColumns;
    }

}
