package com.jbm.test;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.util.archive.Archive;
import com.jbm.util.archive.ExplodedArchive;
import com.jbm.util.archive.JarFileArchive;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.jar.Manifest;

public class JarLauncherTests extends AbstractExecutableArchiveLauncherTests {

    @Test
    public void explodedJarHasOnlyBootInfClassesAndContentsOfBootInfLibOnClasspath() throws Exception {
        File jar = new File("D:\\workspaces\\pvop-platform\\pvop-platform-app\\target\\pvop-platform-app.jar");
//        Archive archive = new JarFileArchive(jar);
        Archive jarfile = findArchive(jar);
        // get the manifest for that file
        Manifest manifest = jarfile.getManifest();
        System.out.println(JSON.toJSONString(manifest.getMainAttributes().getValue("Build-Time")));
    }

    public static Archive findArchive(File file) throws IOException {
        String protocol = FileNameUtil.extName(file);
        if (StrUtil.isBlank(protocol)) {
            URL root = URLUtil.getURL(file);
            protocol = root.getProtocol();
        }
        if (Objects.equals(protocol, "jar")) {
            return new JarFileArchive(file);
        } else {
            return new ExplodedArchive(file);
        }
    }


    public static <T> boolean isStartupFromJar() {
        String protocol = ResourceUtil.getResource("").getProtocol();
        if (Objects.equals(protocol, "jar")) {
            return true;
        } else if (Objects.equals(protocol, "file")) {
            return false;
        }
        return true;
    }
}
