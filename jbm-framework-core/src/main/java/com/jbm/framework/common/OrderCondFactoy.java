package com.jbm.framework.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jbm.framework.metadata.usage.page.OrderCond;

public class OrderCondFactoy {

	private static final Logger logger = LoggerFactory.getLogger(OrderCondFactoy.class);

	/**
	 * 排序列分隔符
	 */
	public static final String COL_SPLITTER = ",";

	/**
	 * 顺序类型分隔符
	 */
	public static final String ORDER_SPLITTER = ":";

	private OrderCondFactoy() {
	}

	public final static List<OrderCond> createOrderCond(String orderStr) {
		return createOrderCond(orderStr, COL_SPLITTER);
	}

	public final static List<OrderCond> createOrderCond(String orderStr, String separatorChars) {
		String[] arr = StringUtils.split(orderStr, separatorChars);
		// Splitter.on(separatorChars).trimResults().omitEmptyStrings().split(separatorChars);
		return createOrderCond(arr);
	}

	public final static List<OrderCond> createOrderCond(String[] orderStr) {
		List<OrderCond> orders = new ArrayList<OrderCond>();
		for (String column : orderStr) {
			if (StringUtils.isBlank(column))
				continue;
			orders.add(new OrderCond(column));
		}
		return orders;
	}

	public final static List<OrderCond> createOrderCond(Map<String, String> sortOrderMap) {
		List<OrderCond> sortCondList = new ArrayList<OrderCond>();
		for (String key : sortOrderMap.keySet()) {
			if (StringUtils.isBlank(key))
				continue;
			sortCondList.add(new OrderCond(key, sortOrderMap.get(key)));
		}
		return sortCondList;
	}

	public final static List<OrderCond> createOrderCondByStandardStr(String orderStr) {
		List<OrderCond> sortCondList = Lists.newArrayList();
		// 将字符串切分为 {"column" => "order"} 的形式
		Map<String, String> sortOrderMap = Maps.newHashMap();
		try {
			sortOrderMap = Splitter.on(COL_SPLITTER).trimResults().omitEmptyStrings().withKeyValueSeparator(ORDER_SPLITTER).split(orderStr);
			sortCondList = createOrderCond(sortOrderMap);
		} catch (Exception e) {
			logger.error("转换Order字符串错误", e);
		}
		return sortCondList;
	}

	public static void main(String args[]) {
		// System.out.println(Splitter.on(COL_SPLITTER).trimResults().omitEmptyStrings().withKeyValueSeparator(ORDER_SPLITTER).split("title:ASC,created:DESC,:,"));
		// List<OrderCond> s =
		// createOrderCondByStandardStr("title:ASC,created:DESC,:,");
		// System.out.println(s);
		List<OrderCond> result = createOrderCondByStandardStr("title:ASC,created:DESC,:");
		for (OrderCond str : result) {// 无重复，无序,存储顺序并无实际意义
			System.out.println(str.getColumn());// D E F G A B C
		}
		// System.out.println(ArrayUtils.toString(Arrays.asList(result)));
	}
}
