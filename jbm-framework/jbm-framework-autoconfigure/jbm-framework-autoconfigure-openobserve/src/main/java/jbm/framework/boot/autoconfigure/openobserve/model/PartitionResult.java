package jbm.framework.boot.autoconfigure.openobserve.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PartitionResult {

    private String traceId;
    private Integer fileNum;
    private Long records;
    private Integer originalSize;
    private Integer compressedSize;
    private Integer maxQueryRange;
//    private List<Partition> partitions = new ArrayList<>();
    private String orderBy;
    private Integer limit;
    private Boolean streamingOutput;
    private Boolean streamingAggs;
    private String streamingId;
//    @Data
//    public static class Partition {
//        private Long begin;
//        private Long end;
//    }


}
