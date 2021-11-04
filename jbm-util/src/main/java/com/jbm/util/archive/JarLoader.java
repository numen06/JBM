package com.jbm.util.archive;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;

public class JarLoader {

    static final String BOOT_INF_CLASSES = "BOOT-INF/classes";

    static final String BOOT_INF_LIB = "BOOT-INF/lib";

    public JarLoader() {
    }

    public Archive findArchive() {
        try {
            URL root = ResourceUtil.getResource("");
            return this.findArchive(root);
        } catch (Exception e) {
            return null;
        }
    }

    public Archive findArchive(URL root) throws IOException, URISyntaxException {
        try {
            String path = root.getPath();
            if (StrUtil.contains(path, BOOT_INF_CLASSES)) {
                path = StrUtil.subBefore(root.toURI().getSchemeSpecificPart(), ".jar!", true) + ".jar";
            }
            File file = FileUtil.file(path);
//        return this.findArchive(new File(path));
            String protocol = root.getProtocol();
            if (Objects.equals(protocol, "jar")) {
                return new JarFileArchive(file, root);
            } else {
                return new ExplodedArchive(file);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public Archive findArchive(File file) {
        try {
            String protocol = FileNameUtil.extName(file);
            if (StrUtil.isBlank(protocol)) {
                URL root = URLUtil.getURL(file);
                return this.findArchive(root);
            }
            if (Objects.equals(protocol, "jar")) {
                return new JarFileArchive(file);
            } else {
                return new ExplodedArchive(file);
            }
        } catch (Exception e) {
            return null;
        }
    }

//    public static void main(String[] args) throws IOException, URISyntaxException {
//        String root = "jar:file:/D:/workspaces/pvop-platform/pvop-platform-app/target/pvop-platform-app.jar!/BOOT-INF/classes!/";
//        JarLoader jarLoader = new JarLoader();
//        Archive archive = jarLoader.findArchive(new URL(root));
//        System.out.println(JSON.toJSONString(archive.getManifest().getMainAttributes()));
//    }


}
