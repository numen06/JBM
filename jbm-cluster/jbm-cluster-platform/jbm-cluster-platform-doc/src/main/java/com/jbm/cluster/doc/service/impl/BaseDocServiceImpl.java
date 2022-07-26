package com.jbm.cluster.doc.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jbm.cluster.api.entitys.doc.BaseDoc;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.cluster.doc.service.BaseDocService;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import com.jbm.util.VersionUtils;
import jbm.framework.boot.autoconfigure.minio.MinioService;
import jodd.io.FileNameUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @Author: wesley.zhang
 * @Create: 2022-07-20 14:46:37
 */
@Service
public class BaseDocServiceImpl extends MasterDataServiceImpl<BaseDoc> implements BaseDocService {

    @Autowired
    private MinioService minioService;

    private static String getExtractPath(final HttpServletRequest request) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return new AntPathMatcher().extractPathWithinPattern(bestMatchPattern, path);
    }

    /**
     * 上传文档
     *
     * @return
     */
    @Override
    public BaseDoc uploadDoc(MultipartFile file, HttpServletRequest request) {
        BaseDoc baseDoc = this.createDoc(file, request);
        try {
            minioService.upload(Paths.get(baseDoc.getDocPath()), file.getInputStream());
        } catch (Exception e) {
            throw new ServiceException("上传文件发生错误");
        }
        this.saveEntity(baseDoc);
        return baseDoc;
    }

    @Override
    public void removeDoc(String filePath) {
        BaseDoc baseDoc = new BaseDoc();
        baseDoc.setDocId(FileNameUtil.getBaseName(filePath));
        try {
            minioService.remove(Paths.get(filePath));
        } catch (Exception e) {
            throw new ServiceException("上传文件错误");
        }
        this.deleteEntity(baseDoc);
    }

    @Override
    public InputStream getDoc(BaseDoc baseDoc) {
        baseDoc.setDocId(FileNameUtil.getBaseName(baseDoc.getDocPath()));
        try {
            InputStream inputStream = minioService.get(Paths.get(baseDoc.getDocPath()));
            BaseDoc dbDoc = this.getById(baseDoc.getDocId());
            if (ObjectUtil.isEmpty(dbDoc)) {
                File file = FileUtil.writeFromStream(inputStream, baseDoc.getDocPath());
                dbDoc = createDoc(file);
                FileUtil.del(file);
                inputStream = minioService.get(Paths.get(baseDoc.getDocPath()));
                this.saveEntity(dbDoc);
            }
            BeanUtil.copyProperties(dbDoc, baseDoc);
            return inputStream;
//            IoUtil.copy(inputStream, outputStream);
        } catch (Exception e) {
            throw new ServiceException("下载文件错误");
        }
    }

    @Override
    public BaseDoc createDoc(File file) {
        BaseDoc baseDoc = new BaseDoc();
        baseDoc.setDocId(FileNameUtil.getBaseName(file.getName()));
        baseDoc.setDocName(file.getName());
        baseDoc.setSize(FileUtil.size(file));
        //文件版本
        baseDoc.setVersion(VersionUtils.create().toString());
        baseDoc.setContentType(FileUtil.getMimeType(file.getName()));
        //文件路径
        baseDoc.setDocPath(file.getName());
        this.setCreator(baseDoc);
        return baseDoc;
    }

    public void setCreator(BaseDoc baseDoc) {
        try {
            baseDoc.setCreator(LoginHelper.getLoginUser().getUserId());
        } catch (Exception e) {

        }
    }

    @Override
    public BaseDoc createDoc(MultipartFile file, HttpServletRequest request) {
        if (ObjectUtil.isEmpty(file)) {
            throw new ServiceException("上传文件为空");
        }
        final String filePath = getExtractPath(request);
        //扩展名
        String extName = FileUtil.extName(file.getOriginalFilename());
        //随机名称
//        String mainName = IdUtil.objectId();


        BaseDoc baseDoc = new BaseDoc();
        baseDoc.setDocId(IdUtil.objectId());

        String fileName = baseDoc.getDocId() + "." + extName;
        baseDoc.setDocName(file.getOriginalFilename());
        baseDoc.setSize(file.getSize());
        //文件版本
        baseDoc.setVersion(VersionUtils.create().toString());
        Path path = Paths.get(filePath, fileName);
        baseDoc.setContentType(FileUtil.getMimeType(path));
        //文件路径
        baseDoc.setDocPath(FileNameUtil.normalize(path.toString(), true));
        this.setCreator(baseDoc);
        return baseDoc;
    }

}