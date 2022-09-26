package com.jbm.cluster.api.factory;

import com.jbm.cluster.api.service.feign.RemoteFileService;
import com.jbm.framework.metadata.bean.ResultBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务降级处理
 *
 * @author wesley.zhang
 */
@Component
public class RemoteFileFallbackFactory implements FallbackFactory<RemoteFileService> {
    private static final Logger log = LoggerFactory.getLogger(RemoteFileFallbackFactory.class);

    @Override
    public RemoteFileService create(Throwable throwable) {
        log.error("文件服务调用失败:{}", throwable.getMessage());
        return new RemoteFileService() {
            @Override
            public ResultBody<String> upload(MultipartFile file) {
                return ResultBody.error("上传文件失败:" + throwable.getMessage());
            }
        };
    }
}
