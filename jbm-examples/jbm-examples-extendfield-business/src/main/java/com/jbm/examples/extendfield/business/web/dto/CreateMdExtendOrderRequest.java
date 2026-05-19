package com.jbm.examples.extendfield.business.web.dto;

import lombok.Data;

import java.util.Map;

@Data
public class CreateMdExtendOrderRequest {

    private String formCode;

    private String orderNo;

    private String title;

    private Map<String, Object> extendData;
}
