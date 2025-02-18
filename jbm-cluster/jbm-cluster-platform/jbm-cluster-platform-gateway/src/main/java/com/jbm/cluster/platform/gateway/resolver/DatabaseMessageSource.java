package com.jbm.cluster.platform.gateway.resolver;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.ds.simple.SimpleDataSource;
import com.jbm.cluster.platform.gateway.config.JdbcDataSourceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.i18n.LocaleContextResolver;

import javax.annotation.Resource;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

@Slf4j
public class DatabaseMessageSource extends AbstractMessageSource implements InitializingBean {

    @Resource
    private LocaleContextResolver localeContextResolver;

    private final SimpleDataSource dataSource;

    public DatabaseMessageSource(JdbcDataSourceProperties dataSourceProperties) {
        this.dataSource = new SimpleDataSource(dataSourceProperties.getUrl(), dataSourceProperties.getUsername(), dataSourceProperties.getPassword());
    }

    public PromptMessage insertMessage(String code, String message, Locale locale) {
        PromptMessage promptMessage = new PromptMessage(code, message, ObjectUtil.defaultIfNull(locale, Locale.CHINESE).toString());
        try {
            Db.use(dataSource).insert(promptMessage.toEntity());
        } catch (SQLException e) {
            log.error("insert message error", e);
        }
        return promptMessage;
    }

    public PromptMessage selectMessage(String code, String locale) {
        try {
            PromptMessage promptMessage = PromptMessage.of(code, locale);
            List<Entity> entities = Db.use(dataSource).find(promptMessage.toEntity());
            if (CollUtil.isEmpty(entities)) {
                return null;
            }
            return CollUtil.getFirst(entities).toBean(PromptMessage.class);
        } catch (SQLException ignored) {
        }
        return null;
    }


    public String resolveCodeWithoutArguments(ServerWebExchange exchange, String code) {
        Locale locale = ObjectUtil.defaultIfNull(localeContextResolver.resolveLocaleContext(exchange).getLocale(), Locale.CHINESE);
        return resolveCodeWithoutArguments(code, locale);
    }

    @Override

    protected MessageFormat resolveCode(@Nullable String code, Locale locale) {
        String message = resolveCodeWithoutArguments(code, locale);
        if (message == null) {
            throw new NoSuchMessageException(code, locale);
        }
        return new MessageFormat(message, locale);
    }

    @Override
    protected String resolveCodeWithoutArguments(@Nullable String code, Locale locale) {
        PromptMessage promptMessage = selectMessage(code, locale.toString());
        if (promptMessage == null) {
            throw new NoSuchMessageException(code, locale);
        }
        return promptMessage.getMessageText();
    }

    /**
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {

    }
}
