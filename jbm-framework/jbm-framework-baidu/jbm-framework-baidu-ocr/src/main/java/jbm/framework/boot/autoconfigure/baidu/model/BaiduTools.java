package jbm.framework.boot.autoconfigure.baidu.model;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.json.JSONObject;

public class BaiduTools {

    public static <T> BaiduResult<T> resultToBean(JSONObject jsonObject, Class<T> result) {
        BaiduResult<T> baiduResult = JSON.parseObject(jsonObject.toString(), new TypeReference<BaiduResult<T>>() {
        });
        return baiduResult;
    }
}
