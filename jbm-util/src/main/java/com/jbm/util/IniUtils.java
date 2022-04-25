package com.jbm.util;

import cn.hutool.core.io.resource.Resource;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ClassLoaderUtil;
import com.jbm.util.ini.IniReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * 对ini文件进行操作
 *
 * @author wesley
 */
public class IniUtils {

    /**
     * 读取配置文件下面的ini文件
     *
     * @param path
     * @return
     * @throws IOException
     */
    public static IniReader loadClassPath(String path) throws IOException {
        Resource file = ResourceUtil.getResourceObj(path);
        return new IniReader(file);
    }

    /**
     * 读取ini文件
     *
     * @param file
     * @return
     * @throws IOException
     */
    public static IniReader load(File file) throws IOException {
        return new IniReader(file);
    }

}
