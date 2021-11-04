package com.jbm.test;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.util.FileCopyUtils;
public class AbstractExecutableArchiveLauncherTests {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    protected File createJarArchive(String name, String entryPrefix) throws IOException {
        File archive = new File(name);
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(archive));
        jarOutputStream.putNextEntry(new JarEntry(entryPrefix + "/"));
        jarOutputStream.putNextEntry(new JarEntry(entryPrefix + "/classes/"));
        jarOutputStream.putNextEntry(new JarEntry(entryPrefix + "/lib/"));
        put(jarOutputStream, entryPrefix, "boo");
        put(jarOutputStream, entryPrefix, "foo");
        put(jarOutputStream, entryPrefix, "aoo");
        jarOutputStream.close();
        return archive;
    }

    public void put(JarOutputStream jarOutputStream,String entryPrefix,String name)throws IOException {
        JarEntry libFoo = new JarEntry(entryPrefix + "/lib/"+name+".jar");
        libFoo.setMethod(ZipEntry.STORED);
        ByteArrayOutputStream fooJarStream = new ByteArrayOutputStream();
        new JarOutputStream(fooJarStream).close();
        libFoo.setSize(fooJarStream.size());
        CRC32 crc32 = new CRC32();
        crc32.update(fooJarStream.toByteArray());
        libFoo.setCrc(crc32.getValue());
        jarOutputStream.putNextEntry(libFoo);
        jarOutputStream.write(fooJarStream.toByteArray());
    }

    protected File explode(File archive) throws IOException {
        File exploded = this.temp.newFolder("exploded");
        JarFile jarFile = new JarFile(archive);
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            File entryFile = new File(exploded, entry.getName());
            if (entry.isDirectory()) {
                entryFile.mkdirs();
            }
            else {
                FileCopyUtils.copy(jarFile.getInputStream(entry), new FileOutputStream(entryFile));
            }
        }
        jarFile.close();
        return exploded;
    }

    protected Set<URL> getUrls(List<Archive> archives) throws MalformedURLException {
        Set<URL> urls = new HashSet<>(archives.size());
        for (Archive archive : archives) {
            urls.add(archive.getUrl());
        }
        return urls;
    }

}
