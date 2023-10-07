package test.util;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.jbm.util.ListUtils;
import com.jbm.util.comparator.MapComparator;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListUtilsTest {
    @Test
    public void last() {
        System.out.println(ListUtils.last(null, null) + "");
        System.out.println(ListUtils.last(ListUtils.newArrayList("1", "2", "3"), "12"));
    }

    @Test
    public void testAll() {
        System.out.println(ListUtils.newArrayList("12312"));
        Map<String, Object> map1 = new HashMap<String, Object>();
        Map<String, Object> map2 = new HashMap<String, Object>();
        Map<String, Object> map3 = new HashMap<String, Object>();
        map1.put("name", 12d);
        map2.put("name", 10d);
        map3.put("name", 11d);
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        list.add(map1);
        list.add(map2);
        list.add(map3);
        ListUtils.sort(list, "name", MapComparator.ASC);
        System.out.println(list.get(0).get("name"));
        System.out.println(JSON.toJSONString(ListUtils.push(Lists.newArrayList("ad"), "123123")));
    }

}
