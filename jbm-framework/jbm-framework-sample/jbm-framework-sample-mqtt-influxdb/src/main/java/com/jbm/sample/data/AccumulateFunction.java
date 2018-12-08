package com.jbm.sample.data;

import java.util.Map;

import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.function.FunctionUtils;
import com.googlecode.aviator.runtime.type.AviatorDouble;
import com.googlecode.aviator.runtime.type.AviatorObject;

public class AccumulateFunction extends AbstractFunction {

	@Override
	public String getName() {
		return "acc";
	}

	@Override
	public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
		double min = FunctionUtils.getNumberValue(arg1, env).doubleValue();
		double max = FunctionUtils.getNumberValue(arg2, env).doubleValue();
		double r = min + max;
		return AviatorDouble.valueOf(r);
	}

}
