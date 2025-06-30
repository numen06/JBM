package jbm.framework.boot.autoconfigure.openobserve.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class QueryResult {

    private String took;
    private Long total;
    private Integer from;
    private Integer size;
    private Long scanSize;
    private Long scanRecords;
    private List<Map<String,Object>> hits = new ArrayList<>();
}
