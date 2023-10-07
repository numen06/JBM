package test.util;

import com.google.common.collect.Lists;
import com.jbm.util.CollectionUtils;
import com.jbm.util.StringUtils;
import com.jbm.util.list.Closure;
import com.jbm.util.list.Derivative;
import org.junit.Test;

import java.util.List;

public class CollectionUitlsTest {
    @Test
    public void test() throws Exception {
        // Map<String, String> map1 = MapUtils.split("t1:1;t2:2");
        // Map<String, String> map2 = MapUtils.split("t1:4;t2:5");
        // @SuppressWarnings("unchecked")
        // List<Map<String, String>> list = Lists.newArrayList(map1, map2);
        // System.out.println(JSON.toJSONString(CollectionUtils.excavateMap(list,
        // "t1")));
        List<Integer> collection = Lists.newArrayList(1, 2, 3);
        CollectionUtils.foreach(collection, new Closure<Integer>() {
            @Override
            public void execute(Integer input) {
                System.out.println(input);
            }
        });
        List<String> targer = Lists.newArrayList();
        CollectionUtils.derivative(collection, targer, new Derivative<Integer, String>() {
            @Override
            public String execute(Integer input) {
                return input.toString();
            }
        });
        System.out.println(StringUtils.toString(targer));

    }
}
