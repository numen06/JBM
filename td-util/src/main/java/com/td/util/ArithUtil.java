package com.td.util;

import java.math.BigDecimal;
import java.text.NumberFormat;

/**
 * 运算工具
 * @author QuYachu
 *
 */
public class ArithUtil {

	//默认除法运算精度
    private static final int DEF_DIV_SCALE = 10;
    
	/**
	 * 加法运算
	 * @param d1
	 * @param d2
	 * @return
	 */
	public static double add(double d1, double d2) {
		BigDecimal b1 = new BigDecimal(Double.toString(d1));
		BigDecimal b2 = new BigDecimal(Double.toString(d2));
		
		return b1.add(b2).doubleValue();
	}
	
	/**
	 * 减法运算
	 * @param d1
	 * @param d2
	 * @return
	 */
	public static double sub(double d1, double d2) {
		BigDecimal b1 = new BigDecimal(Double.toString(d1));
		BigDecimal b2 = new BigDecimal(Double.toString(d2));
		
		return b1.subtract(b2).doubleValue();
	}
	
	/**
	 * 乘法运算
	 * @param d1
	 * @param d2
	 * @return
	 */
	public static double mul(double d1, double d2) {
		BigDecimal b1 = new BigDecimal(Double.toString(d1));
		BigDecimal b2 = new BigDecimal(Double.toString(d2));
		
		return b1.multiply(b2).doubleValue();
	}
	
	/**
	 * 除法运算
	 * @param d1
	 * @param d2
	 * @return
	 */
	public static double div(double d1, double d2) {
		 return div(d1, d2, DEF_DIV_SCALE);
	}
	
	/**
	 * 除法运算
	 * @param d1
	 * @param d2
	 * @param scale
	 * @return
	 */
	public static double div(double d1, double d2, int scale) {
		if(scale<0){
            throw new IllegalArgumentException(
                "The scale must be a positive integer or zero");
        }
        BigDecimal b1 = new BigDecimal(Double.toString(d1));
        BigDecimal b2 = new BigDecimal(Double.toString(d2));
        return b1.divide(b2,scale,BigDecimal.ROUND_HALF_UP).doubleValue();
	}
	
	/**
	 * 小数位四舍五入
	 * @param d1
	 * @param scale
	 * @return
	 */
	public static double round(double d1, int scale) {
		if(scale<0){
			throw new IllegalArgumentException(
					"The scale must be a positive integer or zero");
		}
		
		BigDecimal b = new BigDecimal(Double.toString(d1));
		BigDecimal one = new BigDecimal("1");
	 	return b.divide(one,scale,BigDecimal.ROUND_HALF_UP).doubleValue();
	}
	
	/**
	 * 获取4舍五入的值
	 * @param d
	 * @param scale
	 * @return
	 */
	public static Double scale(double d, int scale) {
		BigDecimal b = new BigDecimal(Double.toString(d));
		return b.setScale(scale, BigDecimal.ROUND_HALF_UP).doubleValue();
	}
	
	public static String valueFormat(Double value){
        NumberFormat format = NumberFormat.getInstance();
        format.setGroupingUsed(false);
        return format.format(value);
        
    }
	
	public static void main(String[] args) {
		System.out.println(scale(94.4980468,3));
	}
}
