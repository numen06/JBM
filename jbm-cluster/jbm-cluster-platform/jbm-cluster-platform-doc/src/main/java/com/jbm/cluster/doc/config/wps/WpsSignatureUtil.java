package com.jbm.cluster.doc.config.wps;

import cn.hutool.http.ContentType;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.codec.binary.Base64.encodeBase64String;


public class WpsSignatureUtil {
    public static String getKeyValueStr(Map<String, String> params) {
        List<String> keys = new ArrayList<String>() {
            {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    add(entry.getKey());
                }
            }
        };
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            String value = params.get(key) + "&";
            sb.append(key).append("=").append(value);
        }
        return sb.toString();
    }

    public static String getSignature(Map<String, String> params, String appSecret) {
        List<String> keys = new ArrayList<String>() {
            {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    add(entry.getKey());
                }
            }
        };

        // 将所有参数按key的升序排序
        keys.sort(String::compareTo);

        // 构造签名的源字符串
        StringBuilder contents = new StringBuilder();
        for (String key : keys) {
            if (key.equals("_w_signature")) {
                continue;
            }
            contents.append(key).append("=").append(params.get(key));
        }
        contents.append("_w_secretkey=").append(appSecret);

        // 进行hmac sha1 签名
        byte[] bytes = HmacUtils.hmacSha1(appSecret.getBytes(), contents.toString().getBytes());

        //字符串经过Base64编码
        String sign = encodeBase64String(bytes);
        try {
            return URLEncoder.encode(sign, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Map<String, String> paramToMap(String paramStr) {
        String[] params = paramStr.split("&");
        return new HashMap<String, String>() {
            {
                for (String param1 : params) {
                    String[] param = param1.split("=");
                    if (param.length >= 2) {
                        String key = param[0];
                        StringBuilder value = new StringBuilder(param[1]);
                        for (int j = 2; j < param.length; j++) {
                            value.append("=").append(param[j]);
                        }
                        put(key, value.toString());
                    }
                }
            }
        };
    }

    /**
     * 生成签名
     *
     * @param action     GET、POST
     * @param url        调用接口的url，转换接口时传入接口地址不带参；查询接口时地址带参数
     * @param contentMd5 通过getMD5方法计算的值
     * @param headerDate 通过getGMTDate方法计算的值
     */
    private static String getSignature(String action, String url, String contentMd5, String headerDate, String convertAppsecret) {
        try {
            URL ur = new URL(url);
            String key = ur.getPath();
            if (!StringUtils.isEmpty(ur.getQuery())) {
                key = key + "?" + ur.getQuery();
            }
            String signStr = action + "\n" + contentMd5 + "\n" + ContentType.JSON.getValue() + "\n" + headerDate + "\n" + key;
            // 进行hmac sha1 签名
            byte[] bytes = HmacUtils.hmacSha1(convertAppsecret.getBytes(), signStr.getBytes());
            return encodeBase64String(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getAuthorization(String action, String url, String contentMd5, String headerDate, String appid, String convertAppsecret) {
        return "WPS " + appid + ":" + getSignature(action, url, contentMd5, headerDate, convertAppsecret); //签名
    }

}
