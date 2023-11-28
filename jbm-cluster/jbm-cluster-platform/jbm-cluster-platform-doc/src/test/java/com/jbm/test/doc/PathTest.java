package com.jbm.test.doc;

import cn.hutool.core.io.file.PathUtil;
import cn.hutool.core.util.URLUtil;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PathTest {

    @Test
    public  void testPath(){
        Path path = Paths.get("", "", "test.png");
        System.out.println(path.toString());
        path = Paths.get(null, "", "test.png");
        System.out.println(path.toString());
        path = Paths.get(null, "/put", "test.png");
        System.out.println(path.toString());
        path = Paths.get(null, "/put/", "/test/test.png");
        System.out.println(path.toString());

    }


}
