package jbm.framework.boot.autoconfigure.openobserve;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class QueryBean {

    private String sql;
    @JSONField(name = "start_time")
    private Long startTime;
    @JSONField(name = "end_time")
    private Long endTime;
    private Integer from;
    private Integer size;

}
