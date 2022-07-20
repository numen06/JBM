import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baidu.aip.face.AipFace;
import com.baidu.aip.face.MatchRequest;
import com.baidu.aip.util.Base64Util;
import jbm.framework.boot.autoconfigure.baidu.model.BaiduResult;
import jbm.framework.boot.autoconfigure.baidu.model.result.MatchResult;
import org.assertj.core.util.Lists;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class FaceMatchTest {
    /**
     * 重要提示代码中所需工具类
     */
    @Test
    public void faceMatch() {
        // 请求url
        try {
            AipFace aipFace = new AipFace("xxx", "xxx", "xxx");
            String image = Base64Util.encode(IoUtil.readBytes(ResourceUtil.getResource("20220718142113.png").openStream()));
//            JSONObject jsonObject = aipFace.addUser(image, "BASE64", "test", "caobin", null);
            String image2 = Base64Util.encode(IoUtil.readBytes(ResourceUtil.getResource("20220718142117.png").openStream()));
            MatchRequest matchRequest1 = new MatchRequest(image, "BASE64");
            MatchRequest matchRequest2 = new MatchRequest(image2, "BASE64");
            JSONObject jsonObject = aipFace.match(Lists.newArrayList(matchRequest1, matchRequest2));
            BaiduResult<MatchResult> baiduResult = JSON.parseObject(jsonObject.toString(), new TypeReference<BaiduResult<MatchResult>>() {
            });
            System.out.println(baiduResult.getResult());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}


