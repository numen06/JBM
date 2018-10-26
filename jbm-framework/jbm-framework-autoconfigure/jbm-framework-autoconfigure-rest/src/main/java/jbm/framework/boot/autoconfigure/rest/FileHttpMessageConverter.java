package jbm.framework.boot.autoconfigure.rest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import jodd.io.FileNameUtil;
import jodd.io.FileUtil;
import jodd.util.MimeTypes;

public class FileHttpMessageConverter extends AbstractHttpMessageConverter<File> {

	public FileHttpMessageConverter() {
		super(MediaType.ALL);
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		return File.class.isAssignableFrom(clazz);
	}

	@Override
	protected File readInternal(Class<? extends File> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
		File file = FileUtil.createTempFile();
		FileUtil.writeStream(file, inputMessage.getBody());
		return file;
	}

	@Override
	protected MediaType getDefaultContentType(File file) {
		String filename = file.getName();
		if (filename != null) {
			String mediaType = MimeTypes.getMimeType(FileNameUtil.getExtension(filename));
			if (StringUtils.hasText(mediaType)) {
				return MediaType.parseMediaType(mediaType);
			}
		}
		return MediaType.APPLICATION_OCTET_STREAM;
	}

	@Override
	protected Long getContentLength(File file, MediaType contentType) throws IOException {
		// Don't try to determine contentLength on InputStreamResource - cannot
		// be read afterwards...
		// Note: custom InputStreamResource subclasses could provide a
		// pre-calculated content length!
		long contentLength = file.length();
		return (contentLength < 0 ? null : contentLength);
	}

	@Override
	protected void writeInternal(File file, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
		FileSystemResource resource = new FileSystemResource(file);
//		if (resource.exists())
			writeContent(resource, outputMessage);
//		else
//			throw new IOException();
	}

	protected void writeContent(Resource resource, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
		try {
			InputStream in = resource.getInputStream();
			try {
				StreamUtils.copy(in, outputMessage.getBody());
			} catch (NullPointerException ex) {
				// ignore, see SPR-13620
			} finally {
				try {
					in.close();
				} catch (Throwable ex) {
					// ignore, see SPR-12999
				}
			}
		} catch (FileNotFoundException ex) {
			// ignore, see SPR-12999
		}
	}
}
