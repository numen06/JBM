package com.jbm.cluster.api.model;

//import com.fasterxml.jackson.annotation.JsonIgnore;

import com.jbm.cluster.api.model.entity.BaseApi;
import com.jbm.util.StringUtils;
import lombok.Data;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author wesley.zhang
 */
@Data
public class IpLimitApi extends BaseApi implements Serializable {
    private static final long serialVersionUID = 1212925216631391016L;
    private Long itemId;
    private Long policyId;
    private String policyName;
    private Integer policyType;

    //    @JsonIgnore
    private String ipAddress;

    private Set<String> ipAddressSet;

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
        if (StringUtils.isNotBlank(ipAddress)) {
            ipAddressSet = new HashSet(Arrays.asList(ipAddress.split(";")));
        }
    }

}
