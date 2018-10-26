package com.jbm.test.devops;

import java.io.File;
import java.nio.file.Paths;

import org.hyperic.sigar.Cpu;
import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import com.alibaba.fastjson.JSON;

public class SigarTest {
	public final static Sigar sigar = initSigar();

	public static void main(String[] args) throws SigarException {
		Cpu cpu = sigar.getCpu();
		System.out.println("idle: " + cpu.getIdle());// 获取整个系统cpu空闲时间
		System.out.println("irq: " + cpu.getIrq());
		System.out.println("nice: " + cpu.getNice());
		System.out.println("soft irq: " + cpu.getSoftIrq());
		System.out.println("stolen: " + cpu.getStolen());
		System.out.println("sys: " + cpu.getSys());
		System.out.println("total: " + cpu.getTotal());
		System.out.println("user: " + cpu.getUser());
		System.out.println("-----------------------");

		Cpu perc = sigar.getCpu();
		System.out.println("cpu %: " + perc.getSys() / perc.getTotal() * 1.0);
		long[] pids = sigar.getProcList();
		for (long pid : pids) {
			ProcState prs = sigar.getProcState(pid);
			System.out.println(JSON.toJSONString(prs));
		}
	}

	private static Sigar initSigar() {
		try {
			// 此处只为得到依赖库文件的目录，可根据实际项目自定义
			String file = Paths.get("sigar", ".sigar_shellrc").toString();
			File classPath = new File(file).getParentFile();
			String path = System.getProperty("java.library.path");
			String sigarLibPath = classPath.getPath();
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
			return new Sigar();
		} catch (Exception e) {
			return null;
		}
	}

	public static boolean isOSWin() {// OS 版本判断
		String OS = System.getProperty("os.name").toLowerCase();
		if (OS.indexOf("win") >= 0) {
			return true;
		} else
			return false;
	}
}
