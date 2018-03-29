package td.framework.test;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PoolUtils;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Test;

public class PoolTest {

	private GenericObjectPool<ConTest> genericObjectPool = new GenericObjectPool<ConTest>(new BasePooledObjectFactory<ConTest>() {

		@Override
		public ConTest create() throws Exception {
			return new ConTest();
		}

		@Override
		public PooledObject<ConTest> wrap(ConTest obj) {
			return new DefaultPooledObject<ConTest>(obj) ;
		}
	},new GenericObjectPoolConfig());

	@Test
	public void testPool() throws IllegalArgumentException, Exception {
		PoolUtils.prefill(genericObjectPool, 5);
		for (int i = 0; i < 100; i++) {
			ConTest test = null;
			try {
				test = genericObjectPool.borrowObject();
				test.link();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if (i%2==0)
					genericObjectPool.returnObject(test);
			}
		}

	}
}
