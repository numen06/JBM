package com.jbm.cluster.doc.service;

import com.jbm.cluster.api.entitys.doc.BaseDoc;
import com.jbm.cluster.doc.common.file.UploadCategory;
import com.jbm.framework.masterdata.service.IMasterDataService;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * @Author: wesley.zhang
 * @Create: 2022-07-20 14:46:37
 */
public interface BaseDocService extends IMasterDataService<BaseDoc> {

    BaseDoc uploadImage(MultipartFile file, BaseDoc baseDoc, HttpServletRequest request);

    BaseDoc uploadDocument(MultipartFile file, BaseDoc baseDoc, HttpServletRequest request);

    /**
     * @deprecated 请使用 {@link #uploadDocument(MultipartFile, BaseDoc, HttpServletRequest)}
     */
    @Deprecated
    BaseDoc uploadDoc(MultipartFile file, BaseDoc baseDoc, HttpServletRequest request);

    void removeDoc(String docId);

    boolean removeByPaths(List<String> paths);

    InputStream getDoc(BaseDoc baseDoc);

    BaseDoc createDoc(File file);

    BaseDoc createDoc(MultipartFile file, BaseDoc baseDoc, HttpServletRequest request);

    BaseDoc createDoc(MultipartFile file, BaseDoc baseDoc, HttpServletRequest request, UploadCategory category);

    List<BaseDoc> findGroupItemsByPath(String groupPath);
}
