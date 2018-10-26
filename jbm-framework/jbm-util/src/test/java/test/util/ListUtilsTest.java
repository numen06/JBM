package test.util;

import org.junit.Test;

import com.jbm.util.ListUtils;

public class ListUtilsTest {
	@Test
	public void last() {
		System.out.println(ListUtils.last(null, null) + "");
		System.out.println(ListUtils.last(ListUtils.newArrayList("1", "2", "3"), "12"));
	}
}
