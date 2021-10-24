package jbm.framework.boot.autoconfigure.dictionary;

import com.jbm.autoconfig.dic.DictionaryScanner;
import com.jbm.autoconfig.dic.DictionaryTemplate;
import com.jbm.autoconfig.dic.EnumScanPackages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import javax.annotation.Resource;

@Configuration
@Order(-1)
@EnableConfigurationProperties({DictionaryProperties.class})
public class DictionaryAutoConfiguration {


    @Resource
    private EnumScanPackages enumScanPackages;

    /**
     * 常量发现和共享
     *
     * @return
     * @throws Exception
     */
    @Bean
    public DictionaryTemplate dictionaryTemplate(DictionaryScanner dictionaryScanner) {
        DictionaryTemplate dictionaryTemplate = new DictionaryTemplate(dictionaryScanner);
        return dictionaryTemplate;
    }

    @Bean
    public DictionaryScanner dictionaryScanner() {
        DictionaryScanner dictionaryScanner = new DictionaryScanner(enumScanPackages);
//        dictionaryScanner.scanner();
        return dictionaryScanner;
    }


}
