package com.jbm.micro.mysql.web.dto;

import lombok.Data;

import java.util.Map;

/**
 * 更新 {@code md_form_row} 的 JSON 载荷（全量替换 payload 列语义）。
 */
@Data
public class UpdateMdFormRowRequest {

    private Map<String, Object> payload;
}
