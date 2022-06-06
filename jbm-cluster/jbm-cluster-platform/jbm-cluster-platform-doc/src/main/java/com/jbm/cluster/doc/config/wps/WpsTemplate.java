package com.jbm.cluster.doc.config.wps;

import com.jbm.cluster.doc.common.file.WebFileUtil;
import com.jbm.cluster.doc.config.RedirectProperties;
import com.jbm.cluster.doc.config.WpsProperties;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

public class WpsTemplate {

    @Getter
    private final WpsProperties wpsProperties;
    @Getter
    private final RedirectProperties redirect;

    @Autowired
    public WpsTemplate(WpsProperties wpsProperties, RedirectProperties redirect) {
        this.wpsProperties = wpsProperties;
        this.redirect = redirect;
    }

    public String getWpsUrl(Map<String, String> values, String fileType, String fileId) {
        String keyValueStr = WpsSignatureUtil.getKeyValueStr(values);
        String signature = WpsSignatureUtil.getSignature(values, wpsProperties.getAppsecret());
        String fileTypeCode = WebFileUtil.getFileTypeCode(fileType);

        return wpsProperties.getDomain() + fileTypeCode + "/" + fileId + "?"
                + keyValueStr + "_w_signature=" + signature;
    }

    public String getTemplateWpsUrl(String fileType, String userId) {
        Map<String, String> values = new HashMap<String, String>() {
            {
                put(redirect.getKey(), redirect.getValue());
                put("_w_appid", wpsProperties.getAppid());
                put("_w_userid", userId);
            }
        };
        String keyValueStr = WpsSignatureUtil.getKeyValueStr(values);
        String signature = WpsSignatureUtil.getSignature(values, wpsProperties.getAppsecret());
        String fileTypeCode = WebFileUtil.getTypeCode(fileType);

        return wpsProperties.getDomain() + fileTypeCode + "/new/0" + "?"
                + keyValueStr + "_w_signature=" + signature;
    }

}
