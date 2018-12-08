package com.jbm.util;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;

/**
 * 路径文件名工具类
 * 
 * @author wesley
 *
 */
public class PathUtils extends org.apache.commons.io.FilenameUtils {
	private static final String EXTENSION_SEPARATOR = ".";
	private static final String FOLDER_SEPARATOR = "/";

	private static final String WINDOWS_FOLDER_SEPARATOR = "\\";

	private static final String TOP_PATH = "..";

	private static final String CURRENT_PATH = ".";

	// private static final String SLASH_TWO = "\\";

	/**
	 * 获取没有扩展名的文件名
	 * 
	 * @param fileName
	 * @return
	 */
	public static String getWithoutExtension(String filePath) {
		return FilenameUtils.getFullPath(filePath) + PathUtils.getBaseName(filePath);
	}

	public static String getFileName(String filePath) {
		return FilenameUtils.getName(filePath);
	}

	/**
	 * 获取扩展名
	 * 
	 * foo.txt --> "txt" a/b/c.jpg --> "jpg" a/b.txt/c --> "" a/b/c --> ""
	 * 
	 * @param fileName
	 * @return
	 */
	public static String getExtension(String filePath) {
		return FilenameUtils.getExtension(filePath);
	}

	/**
	 * 获取扩展名
	 * 
	 * @param fileName
	 * @param capital
	 *            0:不处理,1:大写,2:小写
	 * @return
	 */
	public static String getExtension(String filePath, int capital) {
		String ext = FilenameUtils.getExtension(filePath);
		switch (capital) {
		case 1:
			return ext.toUpperCase();
		case 2:
			return ext.toLowerCase();
		default:
			return ext;
		}
	}

	/**
	 * 判断是否同为扩展名
	 * 
	 * @param fileName
	 * @param ext
	 * @return
	 */
	public static boolean isSameExtension(String filePath, String ext) {
		return FilenameUtils.isExtension(filePath, ext);
	}

	/**
	 * 获得一个标准的扩展名,小写没有"."的
	 * 
	 * @param ext
	 * @return
	 */
	public static String getStandardExtension(String ext) {
		if (StringUtils.isEmpty(ext))
			return "";
		String reslut = "";
		String extTemp = getExtension("*." + ext, 2);
		if (StringUtils.isEmpty(extTemp)) {
			reslut = ext;
		} else {
			reslut = extTemp;
		}
		return reslut.replace(EXTENSION_SEPARATOR, "");
	}

	/**
	 * 随机生成一个文件名
	 * 
	 * @param ext
	 * @return
	 */
	public static String randomFileName(String ext) {
		UUID uuid = UUID.randomUUID();
		if (StringUtils.isEmpty(getStandardExtension(ext)))
			return new StringBuffer(uuid.toString().replace("-", "")).toString();
		else
			return new StringBuffer(uuid.toString().replace("-", "")).append(EXTENSION_SEPARATOR).append(getStandardExtension(ext)).toString();
	}

	/**
	 * 随机生成一个时间戳的文件名 带毫秒
	 * 
	 * @param ext
	 * @return
	 */
	public static String randomTimeFileName(String ext) {
		Long time = System.currentTimeMillis();
		if (StringUtils.isEmpty(getStandardExtension(ext)))
			return TimeUtils.format(time, "yyyyMMddHHmmssSSS");
		else
			return new StringBuffer(TimeUtils.format(time, "yyyyMMddHHmmssSSS")).append(EXTENSION_SEPARATOR).append(getStandardExtension(ext)).toString();
	}

	/**
	 * 判断是否存在扩展名
	 * 
	 * @param fileName
	 * @return
	 */
	public static boolean hasExtension(String filePath) {
		return StringUtils.isNotBlank(FilenameUtils.getExtension(filePath));
	}

	/**
	 * 得到正确的扩展名
	 * 
	 * @param ext
	 * @return
	 */
	public static String trimExtension(String ext) {
		return getExtension(EXTENSION_SEPARATOR + ext);
	}

	/**
	 * 向path中填充扩展名(如果没有或不同的话)
	 * 
	 * @param fileName
	 * @param ext
	 * @return
	 */
	public static String fillExtension(String filePath, String ext) {
		if (StringUtils.isBlank(filePath))
			return "";
		String _p = FilenameUtils.getFullPath(filePath);
		String _f = FilenameUtils.getName(filePath);
		String _e = PathUtils.getStandardExtension(ext);
		return new StringBuffer(_p).append(_f).append(EXTENSION_SEPARATOR).append(_e).toString();
	}

	/**
	 * 判断是否是文件PATH
	 * 
	 * @param filePath
	 * @return
	 */
	public static boolean isFile(String filePath) {
		return hasExtension(filePath);
	}

	/**
	 * 判断是否是文件夹PATH
	 * 
	 * @param filePath
	 * @return
	 */
	public static boolean isFolder(String filePath) {
		return !hasExtension(filePath);
	}

	/**
	 * 链接PATH前后处理，得到准确的链接PATH
	 * 
	 * @param path
	 * @return
	 */
	public static String trimPath(String path) {
		path = FilenameUtils.getFullPath(path + FOLDER_SEPARATOR);
		return path;
	}

	/**
	 * 通过数组完整链接PATH
	 * 
	 * @param paths
	 * @return
	 */
	public static String bulidFullPath(String... paths) {
		StringBuffer sb = new StringBuffer();
		boolean isFile = false;
		for (int i = 0; i < paths.length; i++) {
			String path = paths[i];
			if (StringUtils.isBlank(path))
				continue;
			if (i == paths.length - 1) {
				if (PathUtils.isFile(path)) {
					isFile = true;
				}
			}
			sb.append(trimPath(path));
		}
		return isFile ? PathUtils.getFullPathNoEndSeparator(sb.toString()) : PathUtils.getFullPath(sb.toString());
	}

	public static String rename(String oldFileName, String newFileName) {
		String ext = PathUtils.getExtension(oldFileName);
		String _f = PathUtils.getWithoutExtension(newFileName);
		String _p = PathUtils.getFullPath(oldFileName);
		return new StringBuffer(_p).append(_f).append(EXTENSION_SEPARATOR).append(ext).toString();
	}

	public static String rename(String oldFileName, String newFileName, String ext) {
		String _f = PathUtils.getWithoutExtension(newFileName);
		String _p = PathUtils.getFullPath(oldFileName);
		return new StringBuffer(_p).append(_f).append(EXTENSION_SEPARATOR).append(ext).toString();
	}

	public static String cleanPath(String path) {
		if (path == null) {
			return null;
		}
		String pathToUse = StringUtils.replace(path, WINDOWS_FOLDER_SEPARATOR, FOLDER_SEPARATOR);

		// Strip prefix from path to analyze, to not treat it as part of the
		// first path element. This is necessary to correctly parse paths like
		// "file:core/../core/io/Resource.class", where the ".." should just
		// strip the first "core" directory while keeping the "file:" prefix.
		int prefixIndex = pathToUse.indexOf(":");
		String prefix = "";
		if (prefixIndex != -1) {
			prefix = pathToUse.substring(0, prefixIndex + 1);
			if (prefix.contains("/")) {
				prefix = "";
			} else {
				pathToUse = pathToUse.substring(prefixIndex + 1);
			}
		}
		if (pathToUse.startsWith(FOLDER_SEPARATOR)) {
			prefix = prefix + FOLDER_SEPARATOR;
			pathToUse = pathToUse.substring(1);
		}

		String[] pathArray = StringUtils.delimitedListToStringArray(pathToUse, FOLDER_SEPARATOR);
		List<String> pathElements = new LinkedList<String>();
		int tops = 0;

		for (int i = pathArray.length - 1; i >= 0; i--) {
			String element = pathArray[i];
			if (CURRENT_PATH.equals(element)) {
				// Points to current directory - drop it.
			} else if (TOP_PATH.equals(element)) {
				// Registering top path found.
				tops++;
			} else {
				if (tops > 0) {
					// Merging path element with element corresponding to top
					// path.
					tops--;
				} else {
					// Normal path element found.
					pathElements.add(0, element);
				}
			}
		}

		// Remaining top paths need to be retained.
		for (int i = 0; i < tops; i++) {
			pathElements.add(0, TOP_PATH);
		}

		return prefix + StringUtils.collectionToDelimitedString(pathElements, FOLDER_SEPARATOR);
	}

	/**
	 * Apply the given relative path to the given path, assuming standard Java
	 * folder separation (i.e. "/" separators).
	 * 
	 * @param path
	 *            the path to start from (usually a full file path)
	 * @param relativePath
	 *            the relative path to apply (relative to the full file path
	 *            above)
	 * @return the full file path that results from applying the relative path
	 */
	public static String applyRelativePath(String path, String relativePath) {
		int separatorIndex = path.lastIndexOf(FOLDER_SEPARATOR);
		if (separatorIndex != -1) {
			String newPath = path.substring(0, separatorIndex);
			if (!relativePath.startsWith(FOLDER_SEPARATOR)) {
				newPath += FOLDER_SEPARATOR;
			}
			return newPath + relativePath;
		} else {
			return relativePath;
		}
	}

	/**
	 * Extract the filename from the given path, e.g. "mypath/myfile.txt" ->
	 * "myfile.txt".
	 * 
	 * @param path
	 *            the file path (may be {@code null})
	 * @return the extracted filename, or {@code null} if none
	 */
	public static String getFilename(String path) {
		if (path == null) {
			return null;
		}
		int separatorIndex = path.lastIndexOf(FOLDER_SEPARATOR);
		return (separatorIndex != -1 ? path.substring(separatorIndex + 1) : path);
	}

	public static void main(String[] args) {
		UUID id = UUID.randomUUID();
		System.out.println(PathUtils.randomFileName(""));
		System.out.println(id.toString().replace("-", ""));

		System.out.println(PathUtils.getExtension(".12"));
		System.out.println(PathUtils.getStandardExtension("1s2"));
		System.out.println(PathUtils.getStandardExtension("123123.te"));
		String fileName = "D:/apache-tomcat-7.0.23/webapps/ROOT/report rets/attachment/report/test/212.11111/1";
		System.out.println("----");
		System.out.println(PathUtils.isFile(fileName));
		System.out.println(PathUtils.fillExtension(fileName, "PDF"));
		System.out.println(PathUtils.getWithoutExtension(PathUtils.fillExtension(fileName, "PDF")));
		System.out.println(PathUtils.getFileName(fileName));
		System.out.println(new File(fileName).getPath());
		String[] paths = { "win", "t", "a", "teswe" };
		System.out.println(PathUtils.bulidFullPath(paths));

		System.out.println(PathUtils.rename("d:/1/1/13/./test.test", "1231.11"));
	}
}
