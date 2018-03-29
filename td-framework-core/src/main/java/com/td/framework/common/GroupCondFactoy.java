package com.td.framework.common;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Lists;
import com.td.framework.metadata.usage.page.GroupCond;

public class GroupCondFactoy {

	private GroupCondFactoy() {
	}

	public final static List<GroupCond> createGroupCondByStr(String orderStr) {
		String[] arr = StringUtils.split(orderStr);
		return createGroupCondByArray(arr);
	}

	public final static List<GroupCond> createGroupCondByArray(String[] orderStr) {
		List<GroupCond> orders = Lists.newArrayList();
		for (String column : orderStr) {
			orders.add(new GroupCond(column));
		}
		return orders;
	}
}
