package com.jbm.cluster.doc.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.entitys.doc.BaseDoc;
import com.jbm.cluster.api.entitys.doc.BaseDocToken;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.cluster.doc.service.BaseDocService;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import com.jbm.util.bean.Version;
import jbm.framework.boot.autoconfigure.minio.MinioException;
import jbm.framework.boot.autoconfigure.minio.MinioService;
import jodd.io.FileNameUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

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


    public BaseDoc selectByDocId(String docId) {
        return super.getById(docId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByIds(Collection<?> list) {
        list.stream().forEach(new Consumer<Object>() {
            @Override
            public void accept(Object o) {
                BaseDoc baseDoc = selectByDocId(o.toString());
                try {
                    minioService.remove(Paths.get(baseDoc.getDocPath()));
                } catch (MinioException e) {
                    throw new ServiceException("删除文件失败");
                }
            }
        });
        return super.removeByIds(list);
    }

    /**
     * 上传文档
     *
     * @param file    上传的文件
     * @param baseDoc 基础文档对象
     * @param request HTTP请求对象
     * @return 上传的文档对象
     */
    @Override
    public BaseDoc uploadDoc(MultipartFile file, BaseDoc baseDoc, HttpServletRequest request) {
        baseDoc = this.createDoc(file, baseDoc, request);
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

    /**
     * 创建一个Doc对象
     *
     * @param file 文件对象
     * @return Doc对象
     */
    @Override
    public BaseDoc createDoc(File file) {
        BaseDoc baseDoc = new BaseDoc();
        baseDoc.setDocId(FileNameUtil.getBaseName(file.getName()));
        baseDoc.setDocName(file.getName());
        baseDoc.setSize(FileUtil.size(file));
        // 创建一个版本对象
        baseDoc.setVersion(new Version());
        baseDoc.setContentType(FileUtil.getMimeType(file.getName()));
        // 设置文件路径
        baseDoc.setDocPath(file.getName());
        this.setCreator(baseDoc);
        return baseDoc;
    }


    /**
     * 设置创建者
     *
     * @param baseDoc 基础文档对象
     */
    public void setCreator(BaseDoc baseDoc) {
        try {
            baseDoc.setCreator(LoginHelper.getLoginUser().getUserId());
        } catch (Exception e) {

        }
    }


    /**
     * 创建一个文档对象
     *
     * @param file    上传的文件
     * @param request HTTP请求对象
     * @return 创建的文档对象
     */
    @Override
    public BaseDoc createDoc(MultipartFile file, BaseDoc baseDoc, HttpServletRequest request) {
        if (ObjectUtil.isEmpty(file)) {
            throw new ServiceException("上传文件为空");
        }
        final String filePath = getExtractPath(request);
        // 扩展名
        String extName = FileUtil.extName(file.getOriginalFilename());
        // 随机名称
        // String mainName = IdUtil.objectId();
        if (ObjectUtil.isEmpty(baseDoc)) {
            baseDoc = new BaseDoc();
        }
        baseDoc.setDocId(IdUtil.objectId());
        //如果为空的话置空
        String group = StrUtil.emptyToNull(baseDoc.getDocGroup());
        baseDoc.setDocGroup(group);
        String fileName = baseDoc.getDocId() + "." + extName;
        baseDoc.setDocName(file.getOriginalFilename());
        baseDoc.setSize(file.getSize());
        // 文件版本
        baseDoc.setVersion(new Version());
        // 防止空文件夹
        Path path = Paths.get(StrUtil.nullToDefault(group, ""), filePath, fileName);
        baseDoc.setContentType(FileUtil.getMimeType(path));
        // 文件路径
        baseDoc.setDocPath(FileNameUtil.normalize(path.toString(), true));
        this.setCreator(baseDoc);
        return baseDoc;
    }

    /**
     * 根据路径查找分组项列表
     *
     * @param groupPath 分组路径
     * @return 分组项列表
     */
    @Override
    public List<BaseDoc> findGroupItemsByPath(String groupPath) {
        QueryWrapper<BaseDoc> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(BaseDoc::getDocGroup, groupPath);
        //最多一百条
        queryWrapper.last("LIMIT 100");
        return this.selectEntitysByWapper(queryWrapper);
    }



}