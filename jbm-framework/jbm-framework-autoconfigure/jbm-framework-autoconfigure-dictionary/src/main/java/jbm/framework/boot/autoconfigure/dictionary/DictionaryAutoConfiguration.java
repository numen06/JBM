package jbm.framework.boot.autoconfigure.dictionary;

import com.jbm.autoconfig.dic.DictionaryScanner;
import com.jbm.autoconfig.dic.DictionaryTemplate;
import com.jbm.autoconfig.dic.EnumScanPackages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
//@AutoConfigureBefore(DataSourceAutoConfiguration.class)
//@ConditionalOnProperty(prefix = DictionaryProperties.PREFIX, name = "basepackage")
@EnableConfigurationProperties({DictionaryProperties.class})
public class DictionaryAutoConfiguration {
    // private final static Logger logger =
    // LoggerFactory.getLogger(DictionaryAutoConfiguration.class);

    @Autowired
    private DictionaryProperties dictionaryProperties;

    @Resource
    private EnumScanPackages enumScanPackages;

    // private RealRestTemplate realRestTemplate =
    // RestTemplateFactory.getInstance().createRealRestTemplate();


    /**
     * 常量发现和共享
     *
     * @return
     * @throws Exception
     */
    @Bean
    public DictionaryTemplate dictionaryTemplate() {
        DictionaryTemplate dictionaryTemplate = new DictionaryTemplate();
        DictionaryScanner dictionaryScanner = new DictionaryScanner(dictionaryTemplate, enumScanPackages);
        dictionaryScanner.scanner();
        return dictionaryTemplate;
    }


}
