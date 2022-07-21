package com.jbm.cluster.doc.service;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;

public interface FileOperateServcie {

    void upload(Path source, InputStream file, String contentType);

    void upload(Path source, File file);

    void remove(Path source);

    InputStream get(Path path);


}
