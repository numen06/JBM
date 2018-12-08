package jbm.framework.boot.autoconfigure.rest;

import java.io.File;
import java.nio.charset.Charset;

import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.util.Assert;

public class FileFormHttpMessageConverter extends AllEncompassingFormHttpMessageConverter {
	protected String getFilename(Object part) {
		String fileName;
		if (part instanceof File) {
			fileName = ((File) part).getName();
		} else
			fileName = super.getFilename(part);
		return encodeHeaderFieldParam(fileName, Charset.forName("UTF-8"));
	}

	static String encodeHeaderFieldParam(String input, Charset charset) {
		Assert.notNull(input, "Input String should not be null");
		Assert.notNull(charset, "Charset should not be null");
		if (charset.name().equals("US-ASCII")) {
			return input;
		}
		Assert.isTrue(charset.name().equals("UTF-8") || charset.name().equals("ISO-8859-1"), "Charset should be UTF-8 or ISO-8859-1");
		byte[] source = input.getBytes(charset);
		int len = source.length;
		StringBuilder sb = new StringBuilder(len << 1);
//		sb.append(charset.name());
//		sb.append("''");
		for (byte b : source) {
			if (isRFC5987AttrChar(b)) {
				sb.append((char) b);
			} else {
				sb.append('%');
				char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
				char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
				sb.append(hex1);
				sb.append(hex2);
			}
		}
		return sb.toString();
	}

	private static boolean isRFC5987AttrChar(byte c) {
		return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '!' || c == '#' || c == '$' || c == '&' || c == '+' || c == '-' || c == '.'
			|| c == '^' || c == '_' || c == '`' || c == '|' || c == '~';
	}

}
