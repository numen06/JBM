package jbm.framework.aliyun.oss;

import cn.hutool.core.util.IdUtil;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.BucketInfo;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.InputStream;

/**
 * @program: JBM
 * @author: wesley.zhang
 * @create: 2019-09-12 03:29
 **/
@Slf4j
public class AliyunOSSTemplate {

    private final OSSClient ossClient;

    private final String bucketName;

    public AliyunOSSTemplate(OSSClient ossClient, String bucketName) {
        this.ossClient = ossClient;
        this.bucketName = bucketName;
    }

    /**
     * 初始化节点
     *
     * @return
     */
    @PostConstruct
    public void initBucketInfo() {
        // 判断Bucket是否存在。详细请参看“SDK手册 > Java-SDK > 管理Bucket”。
        // 链接地址是：https://help.aliyun.com/document_detail/oss/sdk/java-sdk/manage_bucket.html?spm=5176.docoss/sdk/java-sdk/init
        if (ossClient.doesBucketExist(bucketName)) {
            log.info("您已经创建Bucket：{}", bucketName);
        } else {
            log.info("您的Bucket不存在，创建Bucket：{}", bucketName);
            // 创建Bucket。详细请参看“SDK手册 > Java-SDK > 管理Bucket”。
            // 链接地址是：https://help.aliyun.com/document_detail/oss/sdk/java-sdk/manage_bucket.html?spm=5176.docoss/sdk/java-sdk/init
            ossClient.createBucket(bucketName);
        }

        // 查看Bucket信息。详细请参看“SDK手册 > Java-SDK > 管理Bucket”。
        // 链接地址是：https://help.aliyun.com/document_detail/oss/sdk/java-sdk/manage_bucket.html?spm=5176.docoss/sdk/java-sdk/init
        BucketInfo info = ossClient.getBucketInfo(bucketName);
        log.info("Bucket {} 的信息如下:", bucketName);
        log.info("数据中心：{}", info.getBucket().getLocation());
        log.info("创建时间：{}", info.getBucket().getCreationDate());
        log.info("用户标志：{}", info.getBucket().getOwner());
    }


    /**
     * 上传流文件
     *
     * @param is
     */
    public String uploadStream(final InputStream is) {
        // 把字符串存入OSS，Object的名称为firstKey。详细请参看“SDK手册 > Java-SDK > 上传文件”。
        // 链接地址是：https://help.aliyun.com/document_detail/oss/sdk/java-sdk/upload_object.html?spm=5176.docoss/user_guide/upload_object
        final String key = IdUtil.objectId();
        ossClient.putObject(bucketName, key, is);
        log.info("Object：{}存入OSS成功。", key);
        return key;
    }


    /**
     * 上传文件
     *
     * @param file
     */
    public String uploadFile(final File file) {
        final String key = IdUtil.objectId();
        ossClient.putObject(bucketName, key, file);
        log.info("Object：{}存入OSS成功。", key);
        return key;
    }

}
