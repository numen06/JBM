package com.jbm.micro.mysql.web.dto;

import lombok.Data;

import java.util.Map;

@Data
public class MdFormRowResponse {

    private Long id;

    private Map<String, Object> payload;
}
