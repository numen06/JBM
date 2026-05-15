package com.jbm.micro.mysql.web.dto;

import lombok.Data;

@Data
public class CreateMdSampleRequest {
    private String name;
    private String formJson;
}
