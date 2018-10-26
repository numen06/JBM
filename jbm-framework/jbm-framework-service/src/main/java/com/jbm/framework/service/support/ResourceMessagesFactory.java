package com.jbm.framework.service.support;

import java.util.Locale;

import org.springframework.context.support.AbstractMessageSource;

import com.jbm.util.StringUtils;

public class ResourceMessagesFactory {

	private AbstractMessageSource messageSource;

	public AbstractMessageSource getMessageSource() {
		return messageSource;
	}

	public void setMessageSource(AbstractMessageSource messageSource) {
		this.messageSource = messageSource;
	}

	private Locale defaultLocale;

	public Locale getDefaultLocale() {
		return defaultLocale;
	}

	public void setDefaultLocale(Locale defaultLocale) {
		this.defaultLocale = defaultLocale;
	}

	private Locale userLocale;

	public Locale getUserLocale() {
		return userLocale == null ? defaultLocale : userLocale;
	}

	public void setUserLocale(Locale userLocale) {
		this.userLocale = userLocale;
	}

	public final String getMessage(Class<?> clazz, String code, Object... args) {
		return messageSource.getMessage(StringUtils.bond(clazz.getName(), StringUtils.DOT, code), args, StringUtils.EMPTY, getUserLocale());
	}

	public final String getMessage(String code, Object... args) {
		return messageSource.getMessage(code, args, StringUtils.EMPTY, getUserLocale());
	}

	public final String getMessage(String code, Object[] args, String defaultMessage) {
		return messageSource.getMessage(code, args, defaultMessage, getUserLocale());
	}
}
