package com.jbm.framework.devops.env.service;

import java.io.File;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import com.github.pfmiles.minvelocity.TemplateUtil;
import com.github.pfmiles.org.apache.commons.lang.StringUtils;
import com.github.pfmiles.org.apache.velocity.Template;
import com.jbm.framework.devops.env.bean.ConfigOption;

import jodd.io.FileUtil;

@Service
public class TempleteService {

	@PostConstruct
	public void init() throws IOException {
		File inFile = new File(ConfigOption.getInstance().getIn());
		File outFile;
		if (StringUtils.isBlank(ConfigOption.getInstance().getOut())) {
			ConfigOption.getInstance().setOut(ConfigOption.getInstance().getIn());
		}
		outFile = new File(ConfigOption.getInstance().getOut());
		System.out.println(System.getenv());
		try {
			String str = FileUtil.readString(inFile);
			Template temp = TemplateUtil.parseStringTemplate(str);
			// StringWriter writer = new StringWriter();
			String out = TemplateUtil.renderTemplate(temp, System.getenv());
			FileUtil.writeString(outFile, out);
		} finally {
			// outWriter.close();
		}
	}

}
