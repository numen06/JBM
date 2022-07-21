package com.jbm.cluster.doc.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.http.ContentType;
import com.google.common.collect.Maps;
import com.jbm.cluster.doc.common.file.WebFileUtil;
import com.jbm.cluster.doc.config.wps.Context;
import com.jbm.cluster.doc.config.wps.WpsTemplate;
import com.jbm.cluster.doc.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
public class WpsFileService {

    @Autowired
    private WpsTemplate wpsTemplate;


    /**
     * 获取预览用URL
     *
     * @param fileUrl    文件url
     * @param checkToken 是否校验token
     */
    public Token getViewUrl(String fileUrl, boolean checkToken) {
        Token t = new Token();
        String fileType = WebFileUtil.getFileTypeByName(fileUrl);
        // fileId使用uuid保证出现同样的文件而是最新文件
        String uuid = IdUtil.fastSimpleUUID();

        Map<String, String> values = new HashMap<String, String>() {
            {
                put("_w_appid", wpsTemplate.getWpsProperties().getAppid());
                if (checkToken) {
                    put("_w_tokentype", "1");
                }
                put(wpsTemplate.getRedirect().getKey(), wpsTemplate.getRedirect().getValue());
                put("_w_userid", "-1");
                put("_w_filepath", fileUrl);
                put("_w_filetype", "web");
            }
        };
        String wpsUrl = wpsTemplate.getWpsUrl(values, fileType, uuid);
        t.setToken(uuid);
        t.setExpires_in(600);
        t.setWpsUrl(wpsUrl);
        return t;
    }

    /**
     * 获取预览用URL
     *
     * @param filePath 文件路径
     * @param userId   用户id
     * @param type     请求预览文件类型
     */
    public Map<String, Object> getFileInfo(String userId, String filePath, String type) {
        if ("web".equalsIgnoreCase(type)) {
            return getWebFileInfo(filePath);
        } else if ("db".equalsIgnoreCase(type)) {
//            return getDbFileInfo(userId);
        }
        return null;
    }

    /**
     * 获取文件元数据
     *
     * @param filePath 文件路径
     */
    private Map<String, Object> getWebFileInfo(String filePath) {
        log.info("_w_filepath:{}", filePath);
        // 构建默认user信息
        FileUserInfo wpsUser = new FileUserInfo(
                "-1", "我", "read", "https://zmfiletest.oss-cn-hangzhou.aliyuncs.com/user0.png"
        );

        int fileSize = WebFileUtil.getFileSize(filePath);

        // 构建文件
        WebFileInfo file = new WebFileInfo(
                MD5.create().digestHex(filePath), WebFileUtil.getFileName(filePath),
                1, fileSize, "-1", new Date().getTime(), filePath,
                // 默认设置为无水印，只读权限
                new UserAclBO(), new WatermarkBO()
        );

        return new HashMap<String, Object>() {
            {
                put("file", file);
                put("user", wpsUser);
            }
        };
    }

    public Map<String, Object> fileSave(MultipartFile file, String w_userid) {
        return Maps.newHashMap();
    }

    public Map<String, Object> fileVersion(Integer version) {
        return Maps.newHashMap();
    }

    /**
     * 文件重命名
     *
     * @param fileName 文件名
     * @param userId   用户id
     */
    public void fileRename(String fileName, String userId) {
        String fileId = Context.getFileId();
//        FileEntity file = this.findOne(fileId);
//        if (file != null) {
//            file.setName(fileName);
//            file.setModifier(userId);
//            Date date = new Date();
//            file.setModify_time(date.getTime());
//            this.update(file);
//        }
    }

    public Map<String, Object> fileNew(MultipartFile file, String w_userid) {
        return Maps.newHashMap();
    }


    private Map<String, String> makeHeader(String headerDate, String contentMd5, String authorization) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, ContentType.JSON.getValue());
        headers.put(HttpHeaders.DATE, headerDate);
        headers.put(HttpHeaders.CONTENT_MD5, contentMd5);//文档上是 "Content-Md5"
        headers.put(HttpHeaders.AUTHORIZATION, authorization);
        return headers;
    }

    public Map<String, Object> fileHistory(FileReqBody fileReqBody) {
        return Maps.newHashMap();
    }
}
