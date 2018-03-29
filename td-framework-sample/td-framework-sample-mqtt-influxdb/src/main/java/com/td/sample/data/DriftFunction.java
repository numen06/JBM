package com.td.sample.data;

import java.math.BigDecimal;
import java.util.Map;

import com.googlecode.aviator.runtime.function.AbstractFunction;
import com.googlecode.aviator.runtime.function.FunctionUtils;
import com.googlecode.aviator.runtime.type.AviatorDouble;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorType;

import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;

public class DriftFunction extends AbstractFunction {

	public double random(double min, double max) {
		// int max = FunctionUtils.getNumberValue(arg1, env).intValue();
		// int min = FunctionUtils.getNumberValue(arg2, env).intValue();
		// 解决生成的随机数不在范围内
		//double s = Math.random() * ((min - 1) / 1) + (max + 1 - min);
		double s = Math.random() * (max - min) + min;
		return s;
	}

	@Override
	public String getName() {
		return "drift";
	}

	@Override
	public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
		double jishu = FunctionUtils.getNumberValue(arg1, env).doubleValue();
		double fudong = 0;
		if (arg2.getAviatorType().equals(AviatorType.String) && arg2.stringValue(env).indexOf("%") > 0) {
			fudong = jishu * Double.valueOf(StringUtils.remove(arg2.stringValue(env), "%")) * 0.01;
		} else {
			fudong = FunctionUtils.getNumberValue(arg2, env).doubleValue();
		}
		double r = random(jishu - fudong, jishu + fudong);
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
