package com.jbm.cluster.api.model.dic;

import com.jbm.cluster.api.model.JbmClusterResource;
import com.jbm.framework.dictionary.JbmDictionary;
import lombok.Data;

import java.util.List;

/**
 * @author wesley
 * @Created wesley.zhang
 * @Date 2022/5/1 1:43
 * @Description TODO
 */
@Data
public class JbmDicResource extends JbmClusterResource {

    private List<JbmDictionary> jbmDictionaryList;
}
