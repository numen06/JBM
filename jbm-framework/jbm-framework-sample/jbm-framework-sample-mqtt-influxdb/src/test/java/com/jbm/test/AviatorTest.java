package com.jbm.test;

import java.util.HashMap;
import java.util.Map;

import com.googlecode.aviator.AviatorEvaluator;
import com.jbm.sample.data.DriftFunction;
import com.jbm.sample.data.RandomFunction;

import junit.framework.TestCase;

public class AviatorTest extends TestCase {
	static {
		AviatorEvaluator.addFunction(new DriftFunction());
		AviatorEvaluator.addFunction(new RandomFunction());
	}

	public void testDif() {
		System.out.println("--dif--");
		Map<String, Object> env = new HashMap<String, Object>();
		env.put("data", 20);
		Object t = AviatorEvaluator.execute("drift(data,5.0)", env);
		System.out.println(t);
		t = AviatorEvaluator.execute("drift(data,5.0,3)", env);
		System.out.println(t);
		t = AviatorEvaluator.execute("drift(data,'5%',5)", env);
		System.out.println(t);
		System.out.println("--dif--");
	}

	public void testAdd() {
		System.out.println("--add--");
		Map<String, Object> env = new HashMap<String, Object>();
		env.put("data", 20);
		Object t = AviatorEvaluator.execute("data+random(1,2)+(123123+123)", env);
		System.out.println(t);
		t = AviatorEvaluator.execute("random(0.65,0.9,2)", env);
		System.out.println(t);
		for (int i = 0; i < 10; i++) {
			env.put("data", AviatorEvaluator.execute("data+random(1,1000)", env));
			System.out.println("data:" + env.get("data"));
		}
		System.out.println("--add--");
	}
}
