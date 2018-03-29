package service.test.clazz;

import java.util.TreeMap;

import com.alibaba.fastjson.JSON;
import com.td.framework.metadata.bean.ResultForm;
import com.td.framework.metadata.usage.bean.AdvMongoBean;
import com.td.masterdata.entity.common.MasterEntity;
import com.td.util.ClassUtils;

import junit.framework.TestCase;

public class ClassTest extends TestCase {

	public void PrimitiveWrapper() {
		System.out.println(ClassUtils.isPrimitiveWrapper(Long.class));
		System.out.println(ClassUtils.isPrimitiveWrapper(String.class));
	}

	public void testRest() {
		ResultForm<String> rest = ResultForm.createSuccessResultForm("test", "上传数据成功");
		System.out.println(JSON.toJSONString(rest));
	}

	public void testJSON() {
		MasterEntity<AdvMongoBean<String>> ent = new MasterEntity<AdvMongoBean<String>>();
		AdvMongoBean<String> mob = new AdvMongoBean<String>();
		mob.setCode("mongoCode");
		mob.setId("mongoId");
		ent.setCode(mob);
		ent.setId(1212l);
		ent.setName("name");
		TreeMap<String, Object> map = new TreeMap<String, Object>();
		// ObjectUtils.foreachObject(ent, map);
		System.out.println(map);
	}

}
