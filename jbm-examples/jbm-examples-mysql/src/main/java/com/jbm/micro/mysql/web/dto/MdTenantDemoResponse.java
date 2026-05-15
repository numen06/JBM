package com.jbm.micro.mysql.web.dto;

import lombok.Data;

@Data
public class MdTenantDemoResponse {

    private Long id;

    private String name;

    private Long tenantId;

    private String remark;
}
