package com.jbm.cluster.api.service.feign;

import com.jbm.cluster.api.factory.RemoteFileFallbackFactory;
import com.jbm.cluster.core.constant.JbmClusterConstants;
import com.jbm.framework.metadata.bean.ResultBody;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务
 *
 * @author wesley.zhang
 */
@FeignClient(contextId = "remoteFileService", value = JbmClusterConstants.DOC_SERVER, fallbackFactory = RemoteFileFallbackFactory.class)
public interface RemoteFileService {
    /**
     * 上传文件
     *
     * @param file 文件信息
     * @return 结果
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResultBody<String> upload(@RequestPart(value = "file") MultipartFile file);
}
