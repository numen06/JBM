package com.jbm.cluster.bigscreen.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.StreamProgress;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.http.HttpDownloader;
import cn.hutool.http.HttpUtil;
import com.jbm.cluster.api.model.entitys.bigscreen.BigscreenView;
import com.jbm.cluster.bigscreen.common.BigscreenConstants;
import com.jbm.cluster.bigscreen.service.BigscreenViewService;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import com.jbm.util.bean.Version;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Paths;

/**
 * @Author: auto generate by jbm
 * @Create: 2021-09-03 17:08:07
 */
@Service
@Slf4j
public class BigscreenViewServiceImpl extends MasterDataServiceImpl<BigscreenView> implements BigscreenViewService {


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
        File viewDir = this.getViewDir(bigscreenView);
        return FileUtil.exist(viewDir);
    }


    private File getViewDir(BigscreenView bigscreenView) {
        File viewDir = Paths.get(BigscreenConstants.ZIP_DIR, bigscreenView.getViewUrl()).toFile();
        return viewDir;
    }

    @Override
    protected int deleteMapperMap(String statement, Object... args) {
        return super.deleteMapperMap(statement, args);
    }

    private File getViewZip(BigscreenView bigscreenView) {
        File zip = Paths.get(BigscreenConstants.ZIP_DIR, bigscreenView.getId() + ".zip").toFile();
        return zip;
    }

    private File downloadZip(final BigscreenView bigscreenView) {
        File zip = this.getViewZip(bigscreenView);
        HttpDownloader.downloadFile(bigscreenView.getResourcePath(), zip, 60, new StreamProgress() {
            @Override
            public void start() {
                log.info("开始下载:{}", bigscreenView.getViewName());
            }

            @Override
            public void progress(long l) {
                log.debug("已下载{}bytes", l);
            }

            @Override
            public void finish() {
                log.info("完成下载:{}", bigscreenView.getViewName());
            }
        });
        return zip;
    }

    private void unZipView(final BigscreenView bigscreenView, File zipFile) {
        File distDir = Paths.get(BigscreenConstants.ZIP_DIR, bigscreenView.getViewUrl()).toFile();
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
        int cot = this.count(parentView);
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
        File zipDir = FileUtil.newFile(BigscreenConstants.ZIP_DIR);
        if (!FileUtil.exist(zipDir)) {
            FileUtil.createTempFile(zipDir);
        }
        File zipFile = null;
        try {
            zipFile = this.downloadZip(bigscreenView);
        } catch (Exception e) {
            throw new ServiceException("下载资源包错误", e);
        }
        this.unZipView(bigscreenView, zipFile);
        //如果不存在视图首页则提示
        if (!FileUtil.exist(Paths.get(BigscreenConstants.ZIP_DIR, bigscreenView.getViewUrl(), "index.html").toFile())) {
            //发生异常清理视图
            this.cleanView(bigscreenView);
            throw new ServiceException("不存在index.html首页文件");
        }
        return bigscreenView;
    }

}
