package com.jbm.util;

import com.jbm.util.bean.Version;

/**
 * @author wesley.zhang
 * @version 1.0
 * 版本号工具类
 * @date 2017年11月7日
 */
public class VersionUtils {


    /**
     * 创建一个版本1.0.0
     *
     * @return
     */
    public static Version create() {
        return Version.create();
    }

    /**
     * 创建一个版本
     *
     * @param ver
     * @return
     */
    public static Version create(String ver) {
        return Version.parse(ver);
    }

    /**
     * 比较是否是之前的版本
     *
     * @param v1
     * @param v2
     * @return
     */
    public static boolean before(Version v1, Version v2) {
        return v1.compareTo(v2) > 0;
    }

    /**
     * 比较是否是之前的版本
     *
     * @param ver1
     * @param ver2
     * @return
     */
    public static boolean before(String ver1, String ver2) {
        Version v1 = Version.parse(ver1);
        Version v2 = Version.parse(ver2);
        return before(v1, v2);
    }

    /**
     * 比较是否是之后的版本
     *
     * @param v1
     * @param v2
     * @return
     */
    public static boolean after(Version v1, Version v2) {
        return v1.compareTo(v2) < 0;
    }

    /**
     * 比较是否是之后的版本
     *
     * @param ver1
     * @param ver2
     * @return
     */
    public static boolean after(String ver1, String ver2) {
        Version v1 = Version.parse(ver1);
        Version v2 = Version.parse(ver2);
        return after(v1, v2);
    }


    /**
     * 相同的版本
     *
     * @param v1
     * @param v2
     * @return
     */
    public static boolean same(Version v1, Version v2) {
        return v1.compareTo(v2) == 0;
    }

    /**
     * 相同的版本
     *
     * @param ver1
     * @param ver2
     * @return
     */
    public static boolean same(String ver1, String ver2) {
        Version v1 = Version.parse(ver1);
        Version v2 = Version.parse(ver2);
        return same(v1, v2);
    }

}
