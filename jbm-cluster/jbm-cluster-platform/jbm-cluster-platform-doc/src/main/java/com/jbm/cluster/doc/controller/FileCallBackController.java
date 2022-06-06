package com.jbm.cluster.doc.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jbm.cluster.doc.model.FileReqBody;
import com.jbm.cluster.doc.service.FileService;
import com.jbm.framework.metadata.bean.ResultBody;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * @author wesley.zhang
 * 文件相关回调接口
 */
@Api(tags = "WPS文件回调接口")
@Slf4j
@RestController
@RequestMapping("/v1/3rd/file")
public class FileCallBackController {


    @Autowired
    private FileService fileService;

    /**
     * 获取文件元数据
     */
    @GetMapping("/info")
    public Object getFileInfo(String _w_userid, String _w_filepath, String _w_filetype) {
        log.info("获取文件元数据userId:{},path:{},type:{}", _w_userid, _w_filepath, _w_filetype);
        try {
            Map<String, Object> map = fileService.getFileInfo(_w_userid, _w_filepath, _w_filetype);
            return map;
        } catch (Exception e) {
            return ResultBody.failed().msg("获取文件元数据异常");
        }
    }

    /**
     * 通知此文件目前有哪些人正在协作
     */
    @PostMapping("/online")
    public ResultBody<Map<String, Object>> fileOnline(@RequestBody JSONObject obj) {
        log.info("通知此文件目前有哪些人正在协作param:{}", JSON.toJSON(obj));
        return ResultBody.ok();
    }

    /**
     * 上传文件新版本
     */
    @PostMapping("/save")
    public ResultBody<Map<String, Object>> fileSave(@RequestBody MultipartFile file, String _w_userid) {
        log.info("上传文件新版本");
        Map<String, Object> map = fileService.fileSave(file, _w_userid);
        return ResultBody.ok().data(map);
    }

    /**
     * 获取特定版本的文件信息
     */
    @GetMapping("version/{version}")
    public ResultBody<Map<String, Object>> fileVersion(@PathVariable Integer version) {
        log.info("获取特定版本的文件信息version:{}", version);
        Map<String, Object> res = fileService.fileVersion(version);
        return ResultBody.ok().data(res);
    }

    /**
     * 文件重命名
     */
    @PutMapping("/rename")
    public ResultBody<Object> fileRename(@RequestBody FileReqBody fileReqBody, String _w_userid) {
        log.info("文件重命名param:{},userId:{}", JSON.toJSON(fileReqBody), _w_userid);
        fileService.fileRename(fileReqBody.getName(), _w_userid);
        return ResultBody.ok();
    }

    /**
     * 获取所有历史版本文件信息
     */
    @PostMapping("/history")
    public ResultBody<Map<String, Object>> fileHistory(@RequestBody FileReqBody fileReqBody) {
        log.info("获取所有历史版本文件信息param:{}", JSON.toJSON(fileReqBody));
        Map<String, Object> result = fileService.fileHistory(fileReqBody);
        return ResultBody.ok().data(result);
    }

    /**
     * 新建文件
     */
    @PostMapping("new")
    public ResultBody<Map<String, Object>> fileNew(@RequestBody MultipartFile file, String _w_userid) {
        log.info("新建文件_w_userid:{}", _w_userid);
        Map<String, Object> res = fileService.fileNew(file, _w_userid);
        return ResultBody.ok().data(res);
    }

//    /**
//     * 文件格式转换回调--wps官方回掉用
//     */
//    @PostMapping("convertCallback")
//    public ResultBody<Object> callback(HttpServletRequest request) {
//        log.info("文件转换回掉");
//        fileService.convertCallBack(request);
//        return ResultBody.ok();
//    }

}
