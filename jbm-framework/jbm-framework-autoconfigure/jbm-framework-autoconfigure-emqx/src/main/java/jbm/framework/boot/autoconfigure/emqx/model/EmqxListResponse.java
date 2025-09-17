package jbm.framework.boot.autoconfigure.emqx.model;

import lombok.Data;

import java.util.List;

/**
 * @author wesley
 */
@Data
public class EmqxListResponse<T> {
    private List<T> data;
    private Meta meta;

    @Data
    public static class Meta {
        private Integer count;
        private Integer limit;
        private Integer page;
    }
}