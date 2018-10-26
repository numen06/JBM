package com.jbm.web.support;

import java.beans.PropertyEditorSupport;
import java.sql.Date;
import java.text.ParseException;

import com.jbm.util.TimeUtil;

public class SqlDateEditor extends PropertyEditorSupport {
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		try {
			setValue(new Date(TimeUtil.parseDate(text).getTime()));
		} catch (ParseException e) {
			throw new IllegalArgumentException("Could not parse Sqldate: " + e.getMessage(), e);
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
