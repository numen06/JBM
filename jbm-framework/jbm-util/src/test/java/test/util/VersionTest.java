package test.util;

import org.junit.Test;

import com.jbm.util.bean.Version;

public class VersionTest {

	@Test
	public void versionAdd() {
		Version ver = new Version(1, 1, 0);
		System.out.println(ver.toString());
		for (int i = 0; i < 100; i++) {
			ver.bugfix();
		}
		System.out.println(ver.toString());
	}

	@Test
	public void versionAdd2() {
		Version ver = new Version(1, 1, 0);
		System.out.println(ver.toString());
		for (int i = 0; i < 100; i++) {
			ver.minor();
		}
		System.out.println(ver.toString());
	}

	@Test
	public void hashcode() {
		System.out.println("----hashcode----");
		// Version ver = new Version(1, 1, 0);
		// System.out.println(ver.toNumber());
		// Version ver1 = new Version(0, 0, 0);
		// System.out.println(ver1.toNumber());
		// Version ver2 = new Version(1, 1, 1);
		// System.out.println(ver2.toNumber());
		// Version ver3 = new Version(2, 2, 1);
		// System.out.println(ver3.toNumber());
		// Version ver4 = new Version(2, 1, 1);
		// System.out.println(ver4.toNumber());
		Version ver = new Version(1, 1, 0);
		for (int i = 0; i < 1; i++) {
			ver.bugfix();
			System.out.println("ver:" + ver.toString());
			System.out.println("number:" + ver.toNumber());
			System.out.println("hashcode:" + ver.hashCode());
		}
	}

	@Test
	public void compareTo() {
		Version ver = new Version(1, 1, 0);
		Version ver1 = new Version(1, 2, 0);
		System.out.println(ver.compareTo(ver1));
	}
}
