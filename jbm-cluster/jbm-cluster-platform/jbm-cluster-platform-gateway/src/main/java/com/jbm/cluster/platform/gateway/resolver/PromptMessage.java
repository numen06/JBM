package com.jbm.cluster.platform.gateway.resolver;

import cn.hutool.db.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Id;

/**
 * @author wesley
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PromptMessage {
    @Id
    private String code;
    private String messageText;
    private String locale;

    public static PromptMessage of(String code, String locale) {
        return new PromptMessage(code, null, locale);
    }

    public static PromptMessage none() {
        return new PromptMessage();
    }

    public static Entity createEntity() {
        return Entity.create("prompt_message");
    }

    public Entity toEntity() {
        return createEntity().parseBean(this, true, true);
    }
}
