package com.jbm.cluster.bigscreen.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.StreamProgress;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.http.HttpDownloader;
import com.jbm.cluster.api.model.entity.bigscreen.BigscreenView;
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
        return !FileUtil.exist(viewDir);
    }


    private File getViewDir(BigscreenView bigscreenView) {
        File viewDir = Paths.get(BigscreenConstants.ZIP_DIR, bigscreenView.getViewUrl()).toFile();
        return viewDir;
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

    @Override
    public BigscreenView saveEntity(BigscreenView bigscreenView) {
        Boolean isNew = ObjectUtil.isEmpty(bigscreenView.getId());
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
        if (this.isUpload(bigscreenView)) {
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
        if (!FileUtil.exist(FileUtil.newFile(BigscreenConstants.ZIP_DIR))) {
            FileUtil.createTempFile(FileUtil.newFile(BigscreenConstants.ZIP_DIR));
        }
        File zipFile = this.downloadZip(bigscreenView);
        this.unZipView(bigscreenView, zipFile);
        return bigscreenView;
    }

}
