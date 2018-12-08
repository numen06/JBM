package com.jbm.web.support;

import java.beans.PropertyEditorSupport;
import java.sql.Timestamp;
import java.text.ParseException;

import com.jbm.util.TimeUtils;

public class SqlTimestampEditor extends PropertyEditorSupport {
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		try {
			setValue(new Timestamp(TimeUtils.parseDate(text).getTime()));
		} catch (ParseException e) {
			throw new IllegalArgumentException("Could not parse Timestamp: " + e.getMessage(), e);
		}
	}

	/**
	 * Format the Date as String, using the specified DateFormat.
	 */
	@Override
	public String getAsText() {
		Timestamp value = (Timestamp) getValue();
		return TimeUtils.format(value.getTime());
	}
}
