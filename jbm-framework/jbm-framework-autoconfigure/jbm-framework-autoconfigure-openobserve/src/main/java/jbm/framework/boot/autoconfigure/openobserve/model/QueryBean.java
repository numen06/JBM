package jbm.framework.boot.autoconfigure.openobserve.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class QueryBean {


    @JSONField(name = "search_type")
    private String searchType = "ui";
    private Integer timeout = 0;
    @JSONField(name = "query")
    private Query query = new Query();

}
