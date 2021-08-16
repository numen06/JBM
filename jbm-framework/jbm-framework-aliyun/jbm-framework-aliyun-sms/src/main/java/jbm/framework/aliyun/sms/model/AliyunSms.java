package jbm.framework.aliyun.sms.model;

import cn.hutool.core.util.ArrayUtil;
import lombok.Data;

import java.util.*;

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
