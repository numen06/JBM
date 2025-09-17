package jbm.framework.boot.autoconfigure.openobserve.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.Map;

@Data
public class QueryBean {


    @JSONField(name = "search_type")
    private String searchType = "ui";
    private Integer timeout = 0;
    @JSONField(name = "query")
    private Query query = new Query();

    private String statement;
    private Map<String, Object> params;

    public static QueryBean defaultQuery() {
        QueryBean queryBean = new QueryBean();
        queryBean.setQuery(new Query());
        queryBean.getQuery().setSize(100);
        queryBean.getQuery().setFrom(0);
        return queryBean;
    }

    public static QueryBean defaultQuery(String statement) {
        QueryBean queryBean = QueryBean.defaultQuery();
        queryBean.setStatement(statement);
        return queryBean;
    }

    public static QueryBean defaultQuery(String statement, Map<String, Object> params) {
        QueryBean queryBean = QueryBean.defaultQuery();
        queryBean.setStatement(statement);
        queryBean.setParams(params);
        return queryBean;
    }

}
