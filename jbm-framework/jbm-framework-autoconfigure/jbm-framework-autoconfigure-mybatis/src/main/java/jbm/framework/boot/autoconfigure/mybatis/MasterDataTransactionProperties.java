package jbm.framework.boot.autoconfigure.mybatis;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jbm.masterdata.transaction")
public class MasterDataTransactionProperties {

    private boolean enabled = true;

    private String servicePointcut = "execution(* com.jbm..service.impl..*(..))";

    private String businessPointcut = "execution(* com.jbm..business.impl..*(..))";
}