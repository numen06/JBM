package com.jbm.framework.devops.actuator.config;

import java.io.File;
import java.nio.file.Paths;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.springframework.core.io.ClassPathResource;

import com.jbm.util.FileUtils;

import jodd.io.FileNameUtil;
import jodd.io.ZipUtil;
import jodd.util.ArraysUtil;

public class SigarService {

	public static Sigar sigar = null;

	public SigarService() {
		super();
		initSigar();
	}

	public static boolean isOSWin() {// OS 版本判断
		String OS = System.getProperty("os.name").toLowerCase();
		if (OS.indexOf("win") >= 0) {
			return true;
		} else
			return false;
	}

	private void initSigar() {
		try {
			File sigarPath = new File("sigar");
			if (!FileUtils.exists(sigarPath)) {
				FileUtils.copyInputStreamToFile(new ClassPathResource("sigar/sigar.zip").getInputStream(), new File("sigar.zip"));
				ZipUtil.unzip("sigar.zip", "sigar/");
			}
			// 此处只为得到依赖库文件的目录，可根据实际项目自定义
			String file = Paths.get("sigar", ".sigar_shellrc").toString();
			File classPath = new File(file).getParentFile();
			String path = System.getProperty("java.library.path");
			String sigarLibPath = FileNameUtil.separatorsToSystem(classPath.toURI().getPath());
			// 为防止java.library.path重复加，此处判断了一下
			if (!path.contains(sigarLibPath)) {
				if (isOSWin()) {
					path += ";" + sigarLibPath;
				} else {
					path += ":" + sigarLibPath;
				}
				System.out.println(path);
				System.setProperty("java.library.path", path);
			}
			sigar = new Sigar();
		} catch (Exception e) {
		}
	}

	public Sigar getSigar() {
		return sigar;
	}

	public boolean existsPid(Integer pid) {
		return existsPid(new Long(pid));
	}

	public boolean existsPid(Long pid) {
		long pids[] = new long[0];
		try {
			pids = sigar.getProcList();
		} catch (SigarException e) {
			return false;
		}
		return ArraysUtil.contains(pids, pid);
	}

}
