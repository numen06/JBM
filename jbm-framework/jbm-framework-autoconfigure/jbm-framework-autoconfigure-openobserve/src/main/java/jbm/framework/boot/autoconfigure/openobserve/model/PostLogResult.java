package jbm.framework.boot.autoconfigure.openobserve.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wesley
 */
@Data
public class PostLogResult {
    //            {
//                "code" : 200,
//                    "status" : [ {
//                "name" : "gateway_logs",
//                        "successful" : 44,
//                        "failed" : 0
//            } ]
//            }
    private Integer code;
    private List<Status> status = new ArrayList<>();

    @Data
    public static class Status {
        private String name;
        private Integer successful;
        private Integer failed;
    }

    public Integer getAllSuccessful() {
        int count = 0;
        for (Status status : status) {
            if (null == status.getSuccessful())
                continue;
            count += status.getSuccessful();
        }
        return count;
    }

    public Integer getAllFailed() {
        int count = 0;
        for (Status status : status) {
            if (null == status.getFailed())
                continue;
            count += status.getFailed();
        }
        return count;
    }
}
