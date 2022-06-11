package com.jbm.cluster.api.model.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @Created wesley.zhang
 * @Date 2022/4/30 17:17
 * @Description TODO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JbmApiResource implements Serializable {

    /**
     * 服务ID
     */
    private String serviceId;

    private List<JbmApi> jbmApiList = new ArrayList<>();

}
