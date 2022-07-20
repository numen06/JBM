import com.baidu.aip.ocr.AipOcr;
import jbm.framework.boot.autoconfigure.baidu.BaiduAutoConfiguration;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.HashMap;

@ExtendWith(SpringExtension.class)
//@Import(BaiduOcrProperties.class)
@SpringBootTest(classes = BaiduAutoConfiguration.class)
public class BaiduOcrTest {

    @Autowired
    private AipOcr client;

    @Value("classpath:image.png")
    private Resource image;

    @Test
    public void testSample() throws IOException {
        // 传入可选参数调用接口
        HashMap<String, String> options = new HashMap<String, String>();
        options.put("language_type", "CHN_ENG");
        options.put("detect_direction", "true");
        options.put("detect_language", "true");
        options.put("probability", "true");

        String url = "https://oss.aliyuncs.com/netmarket/product/1210e531-c002-4177-88df-5673f085db23.png?x-oss-process=image/quality,q_80/format,jpg";

        // 通用文字识别, 图片参数为远程url图片
        JSONObject res = client.basicGeneralUrl(url, options);
        System.out.println(res.toString());

    }

    @Test
    public void testSampleFile() throws IOException {
        // 传入可选参数调用接口
        HashMap<String, String> options = new HashMap<String, String>();
        options.put("language_type", "CHN_ENG");
        options.put("detect_direction", "true");
        options.put("detect_language", "true");
        options.put("probability", "true");

        // 通用文字识别, 图片参数为远程url图片
        JSONObject res = client.basicGeneral(image.getFile().getCanonicalPath(), options);
        System.out.println(res.toString());

    }


}


