package test.cache;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.SerializeUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.jbm.util.cache.Caches;
import com.jbm.util.key.KeyBean;
import com.jbm.util.key.KeyObject;
import com.jbm.util.key.Keys;
import org.junit.Test;
import test.entity.Student;

import java.util.UUID;

public class CacheTest {


    @Test
    public void test() {
        LoadingCache<String, String> cache = Caches.getCachePool()
                .createLoadingCache(
                        (caffeine) -> caffeine.maximumSize(1000),
                        (key) -> UUID.randomUUID().toString());
        cache.put("key", "value");
        System.out.println(cache.get("key"));
    }


    @Test
    public void testKey() {
        LoadingCache<KeyObject, String> cache = Caches.getCachePool()
                .createLoadingCache(
                        (caffeine) -> caffeine.maximumSize(1000),
                        (key) -> UUID.randomUUID().toString());
        String val = IdUtil.fastSimpleUUID();
        cache.put(Keys.of("test"), val);
        Console.log("val:{}", val);
        Console.log("cache:{}", cache.get(Keys.of("test")));
    }

    @Test
    public void testKey2() {
        LoadingCache<KeyBean<String>, String> cache = Caches.getCachePool()
                .createLoadingCache(
                        (caffeine) -> caffeine.maximumSize(1000),
                        (key) -> UUID.randomUUID().toString());
        String val = IdUtil.fastSimpleUUID();
        cache.put(Keys.ofBean("test"), val);
        Console.log("val:{}", val);
        Console.log("cache:{}", cache.get(Keys.ofBean("test")));
    }

    @Test
    public void testKey3() {
        KeyBean<Student> keyBean = Keys.ofBean(new Student(1L, "张三"), Student::getId);
        KeyBean<Student> keyBean2 = Keys.ofBean(new Student(1L, "李四"), Student::getId);
        if (keyBean.equals(keyBean2)) {
            Console.log("equal");
        } else {
            Console.log("not equal");
        }
        LoadingCache<KeyBean<Student>, String> cache = Caches.getCachePool()
                .createLoadingCache(
                        (caffeine) -> caffeine.maximumSize(1000),
                        (key) -> UUID.randomUUID().toString());
        String val = "1";
        cache.put(keyBean, val);
        Console.log("pull val:{}", val);
        Console.log("get val:{}", cache.get(keyBean2));
    }

    @Test
    public void testSer() {
        KeyBean<Student> keyBean = Keys.ofBean(new Student(1L, "张三"), Student::getId);
        String json = JSONObject.toJSONString(keyBean);
        KeyBean<Student> keyBean2 = JSON.parseObject(json, new TypeReference<KeyBean<Student>>() {});
        if (keyBean.equals(keyBean2)) {
            Console.log("json equal");
        } else {
            Console.log("json not equal");
        }
        byte[] b =  SerializeUtil.serialize(keyBean);
        KeyBean<Student> keyBean3 = SerializeUtil.deserialize(b);
        if (keyBean.equals(keyBean3)) {
            Console.log("byte equal");
        } else {
            Console.log("byte not equal");
        }

    }
}
