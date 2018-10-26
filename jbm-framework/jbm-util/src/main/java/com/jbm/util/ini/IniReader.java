package com.jbm.util.ini;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.jbm.util.StringUtils;

import jodd.props.Props;
import jodd.props.PropsEntry;

/**
 * ini文件读取
 * 
 * @author wesley
 *
 */
public class IniReader extends Props {

	private final List<String> sections = new ArrayList<>();

	/**
	 * 构造函数
	 * 
	 * @param filename
	 * @throws IOException
	 */
	public IniReader(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		read(reader);
		reader.close();
		this.load(file);
	}

	/**
	 * 读取文件
	 * 
	 * @param reader
	 * @throws IOException
	 */
	protected void read(BufferedReader reader) throws IOException {
		String line;
		while ((line = reader.readLine()) != null) {
			parseLine(line);
		}
	}

	/**
	 * 解析配置文件行
	 * 
	 * @param line
	 */
	protected void parseLine(String line) {
		line = StringUtils.trimToEmpty(line);
		if (line.matches("\\[.*\\]")) {
			String currentSecion = line.replaceFirst("\\[(.*)\\]", "$1");
			sections.add(currentSecion);
		}
	}

	public List<String> getSections() {
		return sections;
	}

	public Iterator<PropsEntry> getValues(String section) {
		return this.entries().section(section).iterator();
	}

}
