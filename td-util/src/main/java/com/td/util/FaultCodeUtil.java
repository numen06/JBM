package com.td.util;

public final class FaultCodeUtil {
	
	public static String makeFaultCode(String deviceType, String modelCode, String source, String faultCode){
		String ret = "";
		if(deviceType.length() == 1){
			ret = "00";
		}
		else if(deviceType.length() == 2){
			ret = "0";
		}
		if(modelCode == null || "".equals(modelCode.trim())){
			modelCode = "0000";
		}
		ret += deviceType + modelCode + source + "1" + faultCode;
		
		return ret;
	}
	
	public static void main(String[] args){
		String ret = makeFaultCode("1", "0000", "1", "0100");
		
		System.out.println(ret);
	}

}
