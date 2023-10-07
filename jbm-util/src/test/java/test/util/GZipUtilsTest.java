package test.util;

import com.jbm.util.GZipUtils;
import org.junit.Test;

import java.io.IOException;

public class GZipUtilsTest {
    private static String inputStr = "zlex@zlex.org,snowolf@zlex.org,zlex.snowolf@zlex.org";

    @Test
    public void testAll() throws IOException {
        System.err.println("原文:\t" + inputStr);

        byte[] input = inputStr.getBytes();
        System.err.println("长度:\t" + input.length);

        byte[] data = GZipUtils.compress(input);
        System.err.println("压缩后:\t");
        System.err.println("长度:\t" + data.length);

        byte[] output = GZipUtils.decompress(data);
        String outputStr = new String(output);
        System.err.println("解压缩后:\t" + outputStr);
        System.err.println("长度:\t" + output.length);

        // assertEquals(inputStr, outputStr);
    }
}
