package service.test.cache;

import java.util.concurrent.ExecutionException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * 
 * 缓存测试
 * 
 * @author wesley
 *
 */
public class CacheTest {
	private static final LoadingCache<String, String> graphs = CacheBuilder.newBuilder().maximumSize(1000).build(new CacheLoader<String, String>() {
		@Override
		public String load(String key) throws Exception {
			if (key == null)
				System.out.println("key is null");
			return System.currentTimeMillis() + ":" + key;
		}
	});

	public static void main(String[] args) throws ExecutionException {

		for (int i = 20; i < 40; i++) {
			graphs.put(i + "", System.currentTimeMillis() + "-" + i);
			// System.out.println(graphs.get(i + ""));
		}
		for (int i = 0; i < 20; i++) {
			System.out.println(graphs.get(i + ""));
		}

		for (int i = 0; i < 40; i++) {
			System.out.println(graphs.get(i + ""));
		}
		try {
			System.out.println(graphs.get(null));
		} catch (Exception e) {
			System.out.println("null error");
		}
	}
}
