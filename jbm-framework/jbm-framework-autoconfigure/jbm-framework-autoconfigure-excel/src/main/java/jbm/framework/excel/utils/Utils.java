package jbm.framework.excel.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;

import jbm.framework.excel.annotation.ExcelField;
import jbm.framework.excel.handler.ExcelHeader;

public class Utils {

	/**
	 * <p>
	 * 根据JAVA对象注解获取Excel表头信息
	 * </p>
	 * </br>
	 */
	@SuppressWarnings("unchecked")
	static public List<ExcelHeader> getHeaderList(Object obj) {
		List<ExcelHeader> temps = new ArrayList<>();
		List<ExcelHeader> headers = new ArrayList<>();
		if (obj instanceof Class) {
			Class<?> clz = (Class<?>) obj;
			List<Field> fields = new ArrayList<>();
			for (Class<?> clazz = clz; clazz != Object.class; clazz = clazz.getSuperclass()) {
				fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
			}
			int index = 0;
			for (Field field : fields) {
				// 是否使用ExcelField注解
				if (field.isAnnotationPresent(ExcelField.class)) {
					ExcelField er = field.getAnnotation(ExcelField.class);
					headers.add(new ExcelHeader(er.title(), er.order(), field.getName(), field.getType(), er.format()));
				} else {
					index++;
					headers.add(new ExcelHeader(field.getName(), index, field.getName(), field.getType()));
				}
			}
			Collections.sort(headers);
		} else if (obj instanceof List) {
			headers = (List<ExcelHeader>) obj;
		}
		return headers.size() <= 0 ? temps : headers;
	}

	static public Map<Integer, ExcelHeader> getHeaderMap(Row titleRow, Object clz) {
		List<ExcelHeader> headers = getHeaderList(clz);
		Map<Integer, ExcelHeader> maps = new HashMap<>();
		for (Cell c : titleRow) {
			String title = c.getStringCellValue();
			for (ExcelHeader eh : headers) {
				if (eh.getTitle().equals(title.trim())) {
					maps.put(c.getColumnIndex(), eh);
					break;
				}
			}
		}
		return maps;
	}

	static public void outPutFile(Workbook wb, String outFilePath) {
		FileOutputStream fos = null;
		try {
			File f = new File(outFilePath);
			if (f.getParentFile().isDirectory() && !f.getParentFile().exists()) {
				f.mkdirs();
			}
			if (!f.exists()) {
				f.createNewFile();
			}
			fos = new FileOutputStream(outFilePath);
			wb.write(fos);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (fos != null)
					fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	static public String getCellValue(Cell c) {
		String o;
		switch (c.getCellTypeEnum()) {
		case BLANK:
			o = "";
			break;
		case BOOLEAN:
			o = String.valueOf(c.getBooleanCellValue());
			break;
		case FORMULA:
			o = String.valueOf(c.getCellFormula());
			break;
		case NUMERIC:
			if (HSSFDateUtil.isCellDateFormatted(c)) {
				Date date = c.getDateCellValue();
				return String.valueOf(date.getTime());
			} else {
				o = String.valueOf(c.getNumericCellValue());
			}
			break;
		case STRING:
			o = c.getStringCellValue();
			break;
		default:
			o = null;
			break;
		}
		return o;
	}

	static public Object str2TargetClass(String strField, Class<?> clazz) {
		if (null == strField || "".equals(strField))
			return null;
		if ((Long.class == clazz) || (long.class == clazz)) {
			strField = matchDoneBigDecimal(strField);
			strField = RegularUtils.converNumByReg(strField);
			return Long.parseLong(strField);
		}
		if ((Integer.class == clazz) || (int.class == clazz)) {
			strField = matchDoneBigDecimal(strField);
			strField = RegularUtils.converNumByReg(strField);
			return Integer.parseInt(strField);
		}
		if ((Float.class == clazz) || (float.class == clazz)) {
			strField = matchDoneBigDecimal(strField);
			return Float.parseFloat(strField);
		}
		if ((Double.class == clazz) || (double.class == clazz)) {
			strField = matchDoneBigDecimal(strField);
			return Double.parseDouble(strField);
		}
		if ((Character.class == clazz) || (char.class == clazz)) {
			return strField.toCharArray()[0];
		}
		if (Date.class == clazz) {
			return DateUtils.str2DateUnmatch2Null(strField);
		}
		return strField;
	}

	private static String matchDoneBigDecimal(String bigDecimal) {
		// 对科学计数法进行处理
		boolean flg = Pattern.matches("^-?\\d+(\\.\\d+)?(E-?\\d+)?$", bigDecimal);
		if (flg) {
			BigDecimal bd = new BigDecimal(bigDecimal);
			bigDecimal = bd.toPlainString();
		}
		return bigDecimal;
	}
}
