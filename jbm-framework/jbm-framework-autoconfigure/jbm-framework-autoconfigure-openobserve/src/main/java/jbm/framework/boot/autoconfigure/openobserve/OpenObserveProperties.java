package jbm.framework.boot.autoconfigure.openobserve;

import lombok.Data;

@Data
public class OpenObserveProperties {

    private String baseUrl;
    private String organization;
    private String stream;
    private String username;
    private String password;
}
