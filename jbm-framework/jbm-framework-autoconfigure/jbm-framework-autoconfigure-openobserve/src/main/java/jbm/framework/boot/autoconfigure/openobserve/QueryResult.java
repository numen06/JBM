package jbm.framework.boot.autoconfigure.openobserve;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class QueryResult {

    private String took;
    private Long total;
    private Integer from;
    private Integer size;
    private Long scanSize;


    private List<Hit> hits;

    @Data
    static class Hit {
        private Long total;
        private List<Map<String, Object>> hits;
    }
}
