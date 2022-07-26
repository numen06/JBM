package jbm.framework.boot.autoconfigure.dictionary;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = DictionaryProperties.PREFIX)
public class DictionaryProperties {

    public static final String PREFIX = "dictionary";

    public final static String DEF_NAMESPACES = "center";

    /**
     * 常量扫描
     */
    private String basepackage;

    private String application = "application";

    public String getBasepackage() {
        return basepackage;
    }

    public void setBasepackage(String basepackage) {
        this.basepackage = basepackage;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

}
