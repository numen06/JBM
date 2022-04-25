package com.jbm.util.ini;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.Resource;
import cn.hutool.setting.Setting;
import cn.hutool.setting.dialect.Props;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;

import com.jbm.util.StringUtils;


/**
 * ini文件读取
 *
 * @author wesley
 */
public class IniReader {

//    private final List<String> sections = new ArrayList<>();

    private Setting setting;

//    private Props props;

    /**
     * 构造函数
     *
     * @param file
     * @throws IOException
     */
    public IniReader(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        this.setting = new Setting(file, Charsets.UTF_8, true);
        IoUtil.close(reader);
    }

    public IniReader(Resource resource) throws IOException {
        this.setting = new Setting(resource, Charsets.UTF_8, true);
    }


    public List<String> getSections() {
        return this.setting.getGroups();
    }

    public Map<String, String> getValues(String section) {
        return this.setting.getMap(section);
    }

    public String getValue(String key) {
        return this.setting.get(key);
    }

    public String getValue(String section, String key) {
        return this.setting.get(section, key);
    }

}
