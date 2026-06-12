package com.jbm.cluster.api.service.feign;

import com.jbm.cluster.api.factory.RemoteFileFallbackFactory;
import com.jbm.cluster.core.constant.JbmClusterConstants;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件服务
 *
 * @author wesley.zhang
 */
@FeignClient(contextId = "remoteFileService", value = JbmClusterConstants.DOC_SERVER, fallbackFactory = RemoteFileFallbackFactory.class)
public interface RemoteFileService {

    /**
     * 文件服务就绪探针
     */
    @GetMapping("/health/file")
    Map<String, Object> fileHealth();

    /**
     * 下载文件
     */
    @GetMapping("/download/{filePath}")
    ResponseEntity<byte[]> download(@PathVariable("filePath") String filePath);

    /**
     * 上传文件
     *
     * @param file 文件信息
     * @return 结果
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResultBody<String> upload(@RequestPart(value = "file") MultipartFile file);
}
