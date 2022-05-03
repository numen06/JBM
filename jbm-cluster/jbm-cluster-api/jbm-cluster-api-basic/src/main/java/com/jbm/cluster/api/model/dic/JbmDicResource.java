package com.jbm.cluster.api.model.dic;

import com.jbm.framework.dictionary.JbmDictionary;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Created wesley.zhang
 * @Date 2022/5/1 1:43
 * @Description TODO
 */
@Data
@Builder
public class JbmDicResource implements Serializable {

    private List<JbmDictionary> jbmDictionaryList;
}
