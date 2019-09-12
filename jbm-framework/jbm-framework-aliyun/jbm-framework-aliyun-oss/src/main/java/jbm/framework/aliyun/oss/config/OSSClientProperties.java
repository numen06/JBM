package jbm.framework.aliyun.oss.config;

import lombok.Data;

/**
 * @program: JBM
 * @author: wesley.zhang
 * @create: 2019-09-12 02:22
 **/
@Data
public class OSSClientProperties {

    public final static String PREFIX = "aliyun.oss";

    /**
     * Endpoint以杭州为例，其它Region请按实际情况填写
     * http://oss-cn-hangzhou.aliyuncs.com
     */
    private String endpoint = "http://oss.aliyuncs.com";
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    private String clientName;

}
