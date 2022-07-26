package jbm.framework.aliyun.sms.model;

import lombok.Data;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author: create by wesley
 * @date:2019/4/6
 */
@Data
public class AliyunSms {

    private String regionId;

    /**
     * 手机号
     */
    private Set<String> phoneNumbers = new HashSet<>();

    /**
     * 签名
     */
    private String signName;

    /**
     * 模板
     */
    private String templateCode;

    /**
     * 模板参数
     */
    private Map<String, Object> templateParam;

    private String smsUpExtendCode;

    private String outId;

}
