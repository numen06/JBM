package com.td.web.support;

import java.beans.PropertyEditorSupport;
import java.text.ParseException;
import java.util.Date;

import com.td.util.TimeUtil;

public class DateEditor extends PropertyEditorSupport {
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		try {
			setValue(TimeUtil.parseDate(text));
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
		return TimeUtil.format(value);
	}
}
