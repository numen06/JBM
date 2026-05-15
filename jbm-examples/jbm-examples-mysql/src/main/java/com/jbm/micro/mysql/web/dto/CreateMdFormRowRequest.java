package com.jbm.micro.mysql.web.dto;

import lombok.Data;

import java.util.Map;

/**
 * 创建 {@code md_form_row}：动态字段以 JSON（Map）形式提交，由 {@link com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler} 落库。
 */
@Data
public class CreateMdFormRowRequest {

    private Map<String, Object> payload;
}
