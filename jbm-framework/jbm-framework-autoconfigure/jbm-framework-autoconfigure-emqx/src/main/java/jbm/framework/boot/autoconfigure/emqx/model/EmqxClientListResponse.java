package jbm.framework.boot.autoconfigure.emqx.model;

import lombok.Data;

import java.util.List;

/**
 * @author wesley
 */
@Data
public class EmqxClientListResponse {
    private List<EmqxClient> data;
    private Meta meta;

    @Data
    public static class Meta {
        private int count;
        private int limit;
        private int page;
    }
}