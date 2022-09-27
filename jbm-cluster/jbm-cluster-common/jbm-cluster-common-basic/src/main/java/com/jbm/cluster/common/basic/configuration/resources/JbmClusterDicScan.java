package com.jbm.cluster.common.basic.configuration.resources;

import cn.hutool.core.collection.CollUtil;
import com.jbm.autoconfig.dic.DictionaryTemplate;
import com.jbm.cluster.api.model.dic.JbmDicResource;
import com.jbm.cluster.common.basic.configuration.config.JbmClusterProperties;
import com.jbm.cluster.core.constant.QueueConstants;
import com.jbm.framework.dictionary.JbmDictionary;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * @author wesley
 */
@Slf4j
public class JbmClusterDicScan extends JbmClusterResourceScan<JbmDicResource> {

    private final DictionaryTemplate dictionaryTemplate;

    public JbmClusterDicScan(DictionaryTemplate dictionaryTemplate) {
        this.dictionaryTemplate = dictionaryTemplate;
    }


    @Override
    public String queue() {
        return QueueConstants.DIC_RESOURCE_STREAM;
    }

    @Override
    public boolean enable(JbmClusterProperties jbmClusterProperties) {
        return true;
    }

    @Override
    public JbmDicResource scan() {
        JbmDicResource jbmDicResource = new JbmDicResource();
        Map<String, List<JbmDictionary>> dics = dictionaryTemplate.getJbmDicMapCache();
        for (String key : dics.keySet()) {
            List<JbmDictionary> list = dics.get(key);
            if (CollUtil.isEmpty(jbmDicResource.getJbmDictionaryList())) {
                jbmDicResource.setJbmDictionaryList(list);
            } else {
                jbmDicResource.getJbmDictionaryList().addAll(list);
            }
        }
        return jbmDicResource;
    }


}
