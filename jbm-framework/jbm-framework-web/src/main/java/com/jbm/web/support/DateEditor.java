package com.jbm.web.support;

import java.beans.PropertyEditorSupport;
import java.text.ParseException;
import java.util.Date;

import com.jbm.util.TimeUtils;

public class DateEditor extends PropertyEditorSupport {
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		try {
			setValue(TimeUtils.parseDate(text));
		} catch (ParseException e) {
			throw new IllegalArgumentException("Could not parse date: " + e.getMessage(), e);
		}
	}

	/**
	 * Format the Date as String, using the specified DateFormat.
	 */
	@Override
	public String getAsText() {
		Date value = (Date) getValue();
		return TimeUtils.format(value);
	}
}
