package com.jbm.micro.mysql.web.dto;

import lombok.Data;

import java.util.Map;

/**
 * 创建扩展字段演示（也可直接传 formCode + 平铺字段，由 AOP 拆分）。
 */
@Data
public class CreateMdExtendDemoRequest {

    private String formCode;

    private String bizCode;

    private String title;

    private Map<String, Object> extendData;
}
