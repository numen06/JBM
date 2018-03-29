package com.td.sample.data;

import java.math.BigDecimal;
import java.util.Map;

import org.apache.commons.math3.random.RandomDataGenerator;

import com.googlecode.aviator.runtime.function.FunctionUtils;
import com.googlecode.aviator.runtime.type.AviatorDouble;
import com.googlecode.aviator.runtime.type.AviatorObject;

public class RandomFunction extends com.googlecode.aviator.runtime.function.system.RandomFunction {

	public static double random(double min, double max) {
		double randomFloat = new RandomDataGenerator().getRandomGenerator().nextDouble();
		double generatedFloat = min + randomFloat * (max - min);
		return generatedFloat;
	}

	@Override
	public String getName() {
		return "random";
	}

	@Override
	public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
		double min = FunctionUtils.getNumberValue(arg1, env).doubleValue();
		double max = FunctionUtils.getNumberValue(arg2, env).doubleValue();
		double r = random(min, max);
		// double sss = new BigDecimal(r).setScale(2,
		// BigDecimal.ROUND_HALF_UP).doubleValue();
		return AviatorDouble.valueOf(r);
	}

	@Override
	public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2, AviatorObject arg3) {
		AviatorObject data = call(env, arg1, arg2);
		int xiaoshu = FunctionUtils.getNumberValue(arg3, env).intValue();
		BigDecimal s = new BigDecimal(FunctionUtils.getNumberValue(data, env).doubleValue());
		double r = s.setScale(xiaoshu, BigDecimal.ROUND_HALF_UP).doubleValue();
		return AviatorDouble.valueOf(r);
	}

}
