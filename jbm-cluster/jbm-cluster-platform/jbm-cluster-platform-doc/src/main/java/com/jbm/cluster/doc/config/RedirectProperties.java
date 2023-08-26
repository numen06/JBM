package com.jbm.cluster.doc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "redirect")
public class RedirectProperties {

    private String key;
    private String value;

}
