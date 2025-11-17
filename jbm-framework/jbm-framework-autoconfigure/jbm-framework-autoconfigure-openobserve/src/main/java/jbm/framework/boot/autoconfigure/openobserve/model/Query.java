package jbm.framework.boot.autoconfigure.openobserve.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class Query {
    private String sql;
    @JSONField(name = "start_time")
    private Long startTime;
    @JSONField(name = "end_time")
    private Long endTime;
    private Integer from = 0;
    private Integer size = 10;
    private String orderBy = "desc";
    private String sqlMode;
    private Boolean streamingOutput;

}

