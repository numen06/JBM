package com.jbm.cluster.doc.controller;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jbm.cluster.api.entitys.doc.BaseDoc;
import com.jbm.cluster.api.entitys.doc.BaseDocGroup;
import com.jbm.cluster.doc.service.BaseDocGroupService;
import com.jbm.cluster.doc.service.BaseDocService;
import com.jbm.cluster.doc.service.BaseDocTokenService;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataCollection;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.function.Supplier;

/**
 * @Author: auto generate by jbm
 * @Create: 2023-11-28 17:20:20
 */
@Api(tags = "文档分组管理开放接口")
@RestController
@RequestMapping("/baseDocGroup")
public class BaseDocGroupController extends MasterDataCollection<BaseDocGroup, BaseDocGroupService> {

    @Autowired
    private BaseDocGroupService baseDocGroupService;
    @Autowired
    private BaseDocService baseDocService;

    @Autowired
    private BaseDocTokenService baseDocTokenService;

    /**
     * 创建临时组
     */
    @ApiOperation(value = "创建临时组")
    @PostMapping("/createTempGroup")
    public ResultBody<BaseDocGroup> createTempGroup(@RequestBody(required = false) BaseDocGroup baseDocGroup) {
        return ResultBody.callback(() -> {
            return baseDocGroupService.createTempGroup(baseDocGroup);
        });
    }


    /**
     * 查询组内文件
     *
     * @param baseDocGroup 组内文件信息
     * @return 结果对象
     */
    @ApiOperation(value = "查询组内文件")
    @PostMapping("/findGroupItemByToken")
    public ResultBody<List<BaseDoc>> findGroupItemByToken(@RequestBody(required = false) BaseDocGroup baseDocGroup) {
        return ResultBody.callback(() -> {
            if (BooleanUtil.isFalse(baseDocTokenService.checkToken(baseDocGroup.getTokenKey()))) {
                throw new ServiceException("文档token失效或者无效");
            }
            return baseDocGroupService.findGroupItemsByPath(baseDocGroup);
        });
    }

    /**
     * 上传组特定文档
     *
     * @param file    文件对象
     * @param tokenKey   文档分组
     * @param request HTTP请求对象
     * @return 结果信息和文档路径
     */
    @ApiOperation(value = "上传特定文档")
    @PostMapping("/uploadByToken")
    public ResultBody<String> uploadByToken(@RequestParam(value = "file") MultipartFile file, @RequestParam(value = "tokenKey") String tokenKey, HttpServletRequest request) {
        return ResultBody.callback("上传组文档成功", new Supplier<String>() {
            @Override
            public String get() {
                BaseDocGroup group = baseDocGroupService.checkGroupByToken(tokenKey);
//                if (BooleanUtil.isFalse(baseGroupService.checkToken(group))) {
//                    throw new ServiceException("文档token失效或者无效");
//                }
                BaseDoc baseDoc = new BaseDoc();
                baseDoc.setDocGroupId(group.getId());
                baseDoc.setDocGroup(group.getGroupPath());
                return baseDocService.uploadDoc(file, baseDoc, request).getDocPath();
            }
        });
    }


}
