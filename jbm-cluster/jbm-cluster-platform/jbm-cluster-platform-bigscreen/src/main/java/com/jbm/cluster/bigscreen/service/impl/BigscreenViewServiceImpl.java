package com.jbm.cluster.bigscreen.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.StreamProgress;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.http.HttpDownloader;
import cn.hutool.http.HttpUtil;
import com.jbm.cluster.api.entitys.bigscreen.BigscreenView;
import com.jbm.cluster.api.service.feign.RemoteFileService;
import com.jbm.cluster.bigscreen.common.BigscreenConstants;
import com.jbm.cluster.bigscreen.service.BigscreenViewService;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import com.jbm.util.bean.Version;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.nio.file.Paths;
import java.util.List;

/**
 * @Author: wesley.zhang
 * @Create: 2021-09-03 17:08:07
 */
@Service
@Slf4j
public class BigscreenViewServiceImpl extends MasterDataServiceImpl<BigscreenView> implements BigscreenViewService {

    private static final int DOWNLOAD_MAX_RETRIES = 3;
    private static final long DOWNLOAD_RETRY_INTERVAL_MS = 5000L;

    @Resource
    private RemoteFileService remoteFileService;

    /**
     * 加载所有大屏
     * 由 DocServiceReadyListener 在文档文件服务就绪后调用
     */
    @Override
    public void loadAllBigscreens() {
        log.info("开始加载所有大屏");
        try {
            List<BigscreenView> list = this.selectAll();
            for (BigscreenView bigscreenView : list) {
                if (this.isUpload(bigscreenView)) {
                    //跳过已经部署的
                    continue;
                }
                try {
                    this.upload(bigscreenView);
                } catch (Exception e) {
                    log.error("部署大屏{}错误", bigscreenView.getViewName(), e);
                }
            }
            log.info("完成所有大屏加载");
        } catch (Exception e) {
            log.error("部署大屏异常", e);
        }
    }

    @Override
    public boolean hasPendingLoad() {
        List<BigscreenView> list = this.selectAll();
        for (BigscreenView bigscreenView : list) {
            if (!this.isUpload(bigscreenView)) {
                return true;
            }
        }
        return false;
    }

    /***
     * 是否已经上传
     * @param bigscreenView
     * @return
     */
    @Override
    public Boolean isUpload(BigscreenView bigscreenView) {
        if (ObjectUtil.isEmpty(bigscreenView.getId())) {
            throw new ServiceException("ID不能为空");
        }
        bigscreenView = this.getById(bigscreenView.getId());
        return FileUtil.exist(this.getViewIndexFile(bigscreenView));
    }

    private String normalizeViewPath(String viewUrl) {
        if (StrUtil.isBlank(viewUrl)) {
            return viewUrl;
        }
        return StrUtil.removePrefix(viewUrl, "/");
    }

    private File getViewDir(BigscreenView bigscreenView) {
        return Paths.get(BigscreenConstants.ZIP_DIR, normalizeViewPath(bigscreenView.getViewUrl())).toFile();
    }

    private File getViewIndexFile(BigscreenView bigscreenView) {
        return Paths.get(getViewDir(bigscreenView).getPath(), "index.html").toFile();
    }

    private void ensureViewsDir() {
        File zipDir = FileUtil.newFile(BigscreenConstants.ZIP_DIR);
        if (!FileUtil.exist(zipDir)) {
            FileUtil.mkdir(zipDir);
        }
    }


    private File getViewZip(BigscreenView bigscreenView) {
        File zip = Paths.get(BigscreenConstants.ZIP_DIR, bigscreenView.getId() + ".zip").toFile();
        return zip;
    }

    private File downloadZip(final BigscreenView bigscreenView) {
        File zip = this.getViewZip(bigscreenView);
        Exception lastException = null;
        for (int attempt = 1; attempt <= DOWNLOAD_MAX_RETRIES; attempt++) {
            try {
                log.info("开始下载:{}", bigscreenView.getViewName());
                if (isDirectHttpUrl(bigscreenView.getResourcePath())) {
                    HttpDownloader.downloadFile(bigscreenView.getResourcePath(), zip, 60, new StreamProgress() {
                        @Override
                        public void start() {
                        }

                        @Override
                        public void progress(long total, long progressSize) {
                            log.debug("已下载[{}]bytes", progressSize);
                        }

                        @Override
                        public void finish() {
                        }
                    });
                } else {
                    String fileName = resolveDocFileName(bigscreenView);
                    log.info("通过 Feign 下载文档:{}", fileName);
                    ResponseEntity<byte[]> response = remoteFileService.download(fileName);
                    if (response == null || !response.getStatusCode().is2xxSuccessful()
                            || response.getBody() == null || response.getBody().length == 0) {
                        int status = response == null ? -1 : response.getStatusCodeValue();
                        throw new ServiceException("下载文件失败: HTTP " + status);
                    }
                    FileUtil.writeBytes(response.getBody(), zip);
                }
                log.info("完成下载:{}", bigscreenView.getViewName());
                return zip;
            } catch (Exception e) {
                lastException = e;
                log.warn("下载大屏[{}]失败，第{}/{}次: {}", bigscreenView.getViewName(), attempt,
                        DOWNLOAD_MAX_RETRIES, e.getMessage());
                if (attempt < DOWNLOAD_MAX_RETRIES) {
                    ThreadUtil.sleep(DOWNLOAD_RETRY_INTERVAL_MS);
                }
            }
        }
        throw new ServiceException("下载资源包错误", lastException);
    }

    private boolean isDirectHttpUrl(String resourcePath) {
        return HttpUtil.isHttp(resourcePath) || HttpUtil.isHttps(resourcePath);
    }

    private String resolveDocFileName(BigscreenView bigscreenView) {
        if (StrUtil.isBlank(bigscreenView.getResourcePath())) {
            throw new ServiceException("资源路径为空");
        }
        return FileNameUtil.getName(bigscreenView.getResourcePath());
    }

    private void unZipView(final BigscreenView bigscreenView, File zipFile) {
        File distDir = getViewDir(bigscreenView);
        log.info("大屏【{}】的解压路径:{}", bigscreenView.getViewName(), distDir.getAbsolutePath());
        ZipUtil.unzip(zipFile, distDir);
    }

    /***
     * 清理视图
     * @param bigscreenView
     * @return
     */
    @Override
    public void cleanView(BigscreenView bigscreenView) {
        if (ObjectUtil.isEmpty(bigscreenView.getId())) {
            throw new ServiceException("ID不能为空");
        }
        bigscreenView = this.getById(bigscreenView.getId());
        try {
            File zip = this.getViewZip(bigscreenView);
            if (FileUtil.exist(zip)) {
                log.info("清理视图压缩包");
                FileUtil.del(zip);
            }
            File viewDir = this.getViewDir(bigscreenView);
            if (FileUtil.exist(viewDir)) {
                log.info("清理视图文件夹");
                FileUtil.del(viewDir);
            }
        } catch (Exception e) {
            throw new ServiceException("清理视图失败", e);
        }
    }

    @Override
    public boolean deleteEntity(BigscreenView bigscreenView) {
        checkParentDelete(bigscreenView.getId());
        this.cleanView(bigscreenView);
        return super.deleteEntity(bigscreenView);
    }

    private void checkParentDelete(Long parentId) {
        BigscreenView parentView = new BigscreenView();
        parentView.setParentId(parentId);
        Long cot = this.count(parentView);
        if (cot > 0) {
            throw new ServiceException("存在子视图不允许删除");
        }
    }

    @Override
    public boolean deleteById(Long id) {
        checkParentDelete(id);
        BigscreenView bigscreenView = new BigscreenView();
        bigscreenView.setId(id);
        this.cleanView(bigscreenView);
        return super.deleteById(id);
    }


    @Override
    public BigscreenView saveEntity(BigscreenView bigscreenView) {
//        try {
//            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
//            if (StrUtil.isNotBlank(bigscreenView.getResourcePath())) {
//                //如果是HTTP请求
//                if (HttpUtil.isHttp(bigscreenView.getResourcePath()) || HttpUtil.isHttps(bigscreenView.getResourcePath())) {
////如果已经是HTTP请求则放弃处理
//                } else {
//                    String requestUrl = request.getRequestURL().toString();
//                    StringBuffer sb = new StringBuffer("http://");
//                    if (HttpUtil.isHttps(requestUrl)) {
//                        sb = new StringBuffer("https://");
//                    }
//                    sb.append(request.getContextPath());
//                    sb.append(bigscreenView.getResourcePath());
//                    bigscreenView.setResourcePath(sb.toString());
//                }
//            }
//        } catch (Exception e) {
//            log.warn("没有获取到请求");
//        }
        Boolean isNew = ObjectUtil.isEmpty(bigscreenView.getId());
        //如果存在父级节点
        if (!ObjectUtil.isEmpty(bigscreenView.getParentId())) {
            BigscreenView parentView = this.selectById(bigscreenView.getParentId());
            if (ObjectUtil.isEmpty(parentView)) {
                throw new ServiceException("不存在父视图");
            }
            //复制父级节点信息
            bigscreenView.setViewUrl(parentView.getViewUrl());
            bigscreenView.setResourcePath(parentView.getResourcePath());
            if (StrUtil.isBlank(bigscreenView.getViewName())) {
                bigscreenView.setViewName(parentView.getViewName() + "_COPY");
            }
            if (StrUtil.isBlank(bigscreenView.getViewUrl())) {
                bigscreenView.setViewUrl(parentView.getViewUrl());
            }
            if (StrUtil.isBlank(bigscreenView.getStaticParams())) {
                bigscreenView.setStaticParams(parentView.getStaticParams());
            }
            if (StrUtil.isBlank(bigscreenView.getPreviewPicture())) {
                bigscreenView.setPreviewPicture(parentView.getPreviewPicture());
            }
            if (StrUtil.isBlank(bigscreenView.getConfigData())) {
                bigscreenView.setConfigData(parentView.getConfigData());
            }
        }
        if (StrUtil.isBlank(bigscreenView.getResourcePath())) {
            throw new ServiceException("没有上传包");
        }
        if (StrUtil.isBlank(bigscreenView.getViewName())) {
            throw new ServiceException("没有设置大屏名称");
        }
        if (StrUtil.isBlank(bigscreenView.getVersion())) {
            bigscreenView.setVersion(Version.create().toString());
        } else {
            bigscreenView.setVersion(Version.parse(bigscreenView.getVersion()).bugfix().toString());
        }
        //如果是新的大屏则保存一遍
        if (ObjectUtil.isEmpty(bigscreenView.getId())) {
            bigscreenView = super.saveEntity(bigscreenView);
        }
        if (StrUtil.isEmpty(bigscreenView.getStaticParams())) {
            bigscreenView.setStaticParams("id=" + bigscreenView.getId());
        }
//        String tempDir = IdUtil.fastSimpleUUID();
        if (ObjectUtil.isEmpty(bigscreenView.getViewUrl())) {
            bigscreenView.setViewUrl("/" + bigscreenView.getId());
        } else {
            if (StrUtil.lastIndexOfIgnoreCase(bigscreenView.getViewUrl(), "/") > 0) {
                throw new ServiceException("不是合法地址:/xxxx");
            }
        }

        bigscreenView = super.saveEntity(bigscreenView);
        //判断没有解包的话就重新解包一下
        if (!this.isUpload(bigscreenView)) {
            this.upload(bigscreenView);
        }
        return bigscreenView;
    }


    @Override
    public BigscreenView upload(BigscreenView bigscreenView) {
        //如果没有ID是第一次上传
        if (ObjectUtil.isEmpty(bigscreenView.getId())) {
            throw new ServiceException("ID不能为空");
        }
        bigscreenView = this.getById(bigscreenView.getId());
        ensureViewsDir();
        File zipFile;
        try {
            zipFile = this.downloadZip(bigscreenView);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("下载资源包错误", e);
        }
        this.unZipView(bigscreenView, zipFile);
        if (!FileUtil.exist(getViewIndexFile(bigscreenView))) {
            //发生异常清理视图
            this.cleanView(bigscreenView);
            throw new ServiceException("不存在index.html首页文件");
        }
        return bigscreenView;
    }


}
