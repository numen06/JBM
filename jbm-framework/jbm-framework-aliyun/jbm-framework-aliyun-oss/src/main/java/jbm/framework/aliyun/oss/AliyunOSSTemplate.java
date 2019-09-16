package jbm.framework.aliyun.oss;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.BucketInfo;
import com.jbm.util.PathUtils;
import jodd.io.FileNameUtil;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.InputStream;
import java.text.MessageFormat;

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

    public String getBucketName() {
        return bucketName;
    }

    public OSSClient getOssClient() {
        return ossClient;
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
    public String uploadOpenStream(final InputStream is) {
        // 把字符串存入OSS，Object的名称为firstKey。详细请参看“SDK手册 > Java-SDK > 上传文件”。
        // 链接地址是：https://help.aliyun.com/document_detail/oss/sdk/java-sdk/upload_object.html?spm=5176.docoss/user_guide/upload_object
        final String key = IdUtil.objectId();
        ossClient.putObject(bucketName, key, is);
        log.info("Object：{}存入OSS成功。", key);
        return key;
    }


    /**
     * 上传流文件
     *
     * @param is
     */
    public String uploadOpenStream(final InputStream is, final String originalFilename) {
        // 把字符串存入OSS，Object的名称为firstKey。详细请参看“SDK手册 > Java-SDK > 上传文件”。
        // 链接地址是：https://help.aliyun.com/document_detail/oss/sdk/java-sdk/upload_object.html?spm=5176.docoss/user_guide/upload_object
        final String path = newFileName(null, originalFilename);
        ossClient.putObject(bucketName, path, is);
        log.info("Object：{}存入OSS成功。", path);
        return this.buildUrl(path);
    }

    /**
     * 上传流文件
     *
     * @param is
     */
    public String uploadOpenStream(final InputStream is, final String group, final String originalFilename) {
        // 把字符串存入OSS，Object的名称为firstKey。详细请参看“SDK手册 > Java-SDK > 上传文件”。
        // 链接地址是：https://help.aliyun.com/document_detail/oss/sdk/java-sdk/upload_object.html?spm=5176.docoss/user_guide/upload_object
        final String path = newFileName(group, originalFilename);
        ossClient.putObject(bucketName, path, is);
        log.info("Object：{}存入OSS成功。", path);
        return this.buildUrl(path);
    }

    /**
     * 上传文件
     *
     * @param file
     */
    public String uploadOpenFile(final File file) {
        final String path = file.getPath();
        ossClient.putObject(bucketName, path, file);
        log.info("Object：{}存入OSS成功。", path);
        return this.buildUrl(path);
    }

    /**
     * 构造一个新的文件名
     *
     * @param group
     * @param originalFilename
     * @return
     */
    private String newFileName(String group, String originalFilename) {
        if (StrUtil.isBlank(originalFilename)) {
            return IdUtil.objectId();
        }
        String fileName = originalFilename;
        String extend = PathUtils.getExtension(fileName);// 获取文件扩展名
        String newFileName = IdUtil.objectId() + "." + extend;
        return FileNameUtil.concat(StrUtil.trimToEmpty(group), newFileName, true);

    }


    /**
     * 创建一个链接
     *
     * @param path
     * @return
     */
    private String buildUrl(String path) {
        String temp = "https://{0}.{1}/{2}";
        final String url = MessageFormat.format(temp, this.bucketName, ossClient.getEndpoint().getHost(), path);
        return URLUtil.encode(url);
    }

}
