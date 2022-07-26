package com.jbm.cluster.api.model.api;

import com.jbm.cluster.api.model.JbmClusterResource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wesley
 * @Created wesley.zhang
 * @Date 2022/4/30 17:17
 * @Description TODO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JbmApiResource extends JbmClusterResource {

    private List<JbmApi> jbmApiList = new ArrayList<>();

}
