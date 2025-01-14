package jbm.framework.boot.autoconfigure.td.configuration;


import jbm.framework.boot.autoconfigure.td.TdTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * @author wesley
 */
@Slf4j
@EnableConfigurationProperties(TDProperties.class)
@ConditionalOnProperty(prefix = "spring.data.td", name = "url")
public class TDConfiguration {
    @Autowired
    private TDProperties tdProperties;

    @Bean
    public TdTemplate tdTemplate() {
        return new TdTemplate(tdProperties);
    }

//    @Bean("tdDataSource")
//    public DataSource getDataSource(TdTemplate tdTemplate) throws SQLException {
//        return tdTemplate().getDataSource();
//    }

}
