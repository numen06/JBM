package com.jbm.cluster.common.basic.configuration;

import com.jbm.autoconfig.dic.DictionaryTemplate;
import com.jbm.cluster.common.basic.JbmClusterTemplate;
import com.jbm.cluster.common.basic.configuration.config.JbmClusterProperties;
import com.jbm.cluster.common.basic.module.JbmClusterNotification;
import com.jbm.cluster.common.basic.module.JbmClusterStreamTemplate;
import com.jbm.cluster.common.basic.runtime.BasicUnknownRuntimeExceptionFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * @author wesley
 * @Created wesley.zhang
 * @Date 2022/4/27 2:46
 * @Description TODO
 */
@EnableConfigurationProperties({JbmClusterProperties.class})
public class JbmBasicConfiguration {
    @Autowired
    private JbmClusterProperties jbmClusterProperties;
//    @Autowired
//    private BusProperties busProperties;

    @Bean
    public JbmClusterNotification jbmClusterNotification() {
        return new JbmClusterNotification();
    }

    @Bean
    public JbmApiResourceScan jbmApiResourceScan() {
        return new JbmApiResourceScan();
    }

    @Bean
    public JbmClusterJobScan jbmClusterJobScan() {
        return new JbmClusterJobScan();
    }

    @Bean
    public JbmClusterTemplate jbmClusterTemplate() {
        return new JbmClusterTemplate();
    }

    @Bean
    public JbmClusterStreamTemplate jbmClusterStreamTemplate() {
        JbmClusterStreamTemplate jbmClusterStreamTemplate = new JbmClusterStreamTemplate();
        return jbmClusterStreamTemplate;
    }

    @Bean
    public BasicUnknownRuntimeExceptionFilter basicUnknownRuntimeExceptionFilter() {
        return new BasicUnknownRuntimeExceptionFilter();
    }


    @Bean
    @ConditionalOnBean(DictionaryTemplate.class)
    public ClusterDicScan clusterDicScan(DictionaryTemplate dictionaryTemplate) {
        ClusterDicScan scan = new ClusterDicScan(dictionaryTemplate);
        return scan;
    }

}
