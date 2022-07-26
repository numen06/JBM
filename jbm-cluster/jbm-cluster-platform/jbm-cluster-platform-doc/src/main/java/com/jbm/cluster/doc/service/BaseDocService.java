package com.jbm.cluster.doc.service;

import com.jbm.cluster.api.entitys.doc.BaseDoc;
import com.jbm.framework.masterdata.service.IMasterDataService;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.InputStream;

/**
 * @Author: wesley.zhang
 * @Create: 2022-07-20 14:46:37
 */
public interface BaseDocService extends IMasterDataService<BaseDoc> {
    BaseDoc uploadDoc(MultipartFile file, HttpServletRequest request);

    void removeDoc(String docId);

    InputStream getDoc(BaseDoc baseDoc);

    BaseDoc createDoc(File file);

    BaseDoc createDoc(MultipartFile file, HttpServletRequest request);
}
