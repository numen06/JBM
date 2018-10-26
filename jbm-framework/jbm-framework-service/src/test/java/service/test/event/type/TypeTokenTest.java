package service.test.event.type;

import com.google.common.reflect.TypeToken;
import com.jbm.framework.event.bean.RemoteEventBean;

public class TypeTokenTest {

	public static void main(String[] args) {
		TypeToken<RemoteEventBean> stringTok = TypeToken.of(RemoteEventBean.class);
		System.out.println(stringTok.getTypes());
	}
}
