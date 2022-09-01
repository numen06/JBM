package jbm.framework.weixin;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@Slf4j
@EnableConfigurationProperties(WxMenuProperties.class)
public class WxMenuConfiguration {


    @Autowired
    private WxMenuProperties wxMenuProperties;

    @Bean
    public WxMenuTamplate wxMenuTamplate() {
        WxMenuTamplate wxMenuTamplate = new WxMenuTamplate(wxMenuProperties);
        return wxMenuTamplate;
    }
}
