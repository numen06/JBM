package com.jbm.cluster.doc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jbm.cluster.api.entitys.doc.BaseDoc;
import com.jbm.cluster.api.entitys.doc.BaseDocGroup;
import com.jbm.cluster.api.entitys.doc.BaseDocToken;
import com.jbm.cluster.api.form.doc.DocPathForm;
import com.jbm.cluster.common.security.annotation.PermitAll;
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
    private final static String DEF_TOKEN_KEY_HEAD = "Doc-Token-Key";

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
    @PermitAll
    @ApiOperation(value = "查询组内文件")
    @PostMapping("/findGroupItemByToken")
    public ResultBody<List<BaseDoc>> findGroupItemByToken(@RequestHeader(DEF_TOKEN_KEY_HEAD) String tokenKey, @RequestBody(required = false) BaseDocGroup baseDocGroup) {
        return ResultBody.callback("查询组内文件成功", () -> {
            BaseDocToken baseDocToken2 = baseDocTokenService.checkToken(tokenKey);
            if (ObjectUtil.isNull(baseDocToken2)) {
                throw new ServiceException("文档token失效或者无效");
            }
            return baseDocGroupService.findGroupItems(baseDocToken2.getDocGroupId());
        });
    }

    @PermitAll
    @ApiOperation(value = "删除组内文件")
    @PostMapping("/removeGroupItemByToken")
    public ResultBody<Boolean> removeGroupItemByToken(@RequestHeader(DEF_TOKEN_KEY_HEAD) String tokenKey, @RequestBody(required = false) DocPathForm docPathForm) {
        return ResultBody.callback("删除组内文件成功", () -> {
            BaseDocToken baseDocToken2 = baseDocTokenService.checkToken(tokenKey);
            if (ObjectUtil.isNull(baseDocToken2)) {
                throw new ServiceException("文档token失效或者无效");
            }
            if (CollUtil.isNotEmpty(docPathForm.getPaths())) {
                return baseDocGroupService.removeGroupItemsByPath(docPathForm.getPaths());
            } else if (CollUtil.isNotEmpty(docPathForm.getIds())) {
                return baseDocGroupService.removeGroupItemsById(docPathForm.getIds());
            } else {
                throw new ServiceException("没有填写任何参数");
            }
        });
    }


    /**
     * 上传组特定文档
     *
     * @param file     文件对象
     * @param tokenKey 文档分组
     * @param request  HTTP请求对象
     * @return 结果信息和文档路径
     */
    @PermitAll
    @ApiOperation(value = "上传特定文档")
    @PostMapping("/uploadByToken")
    public ResultBody<String> uploadByToken(@RequestHeader(DEF_TOKEN_KEY_HEAD) String tokenKey, @RequestParam(value = "file") MultipartFile file, HttpServletRequest request) {
        return ResultBody.callback("上传组文档成功", new Supplier<String>() {
            @Override
            public String get() {
                BaseDocToken baseDocToken2 = baseDocTokenService.checkToken(tokenKey);
                if (ObjectUtil.isNull(baseDocToken2)) {
                    throw new ServiceException("文档token失效或者无效");
                }
                BaseDocGroup group = baseDocGroupService.getById(baseDocToken2.getDocGroupId());
//                if (BooleanUtil.isFalse(baseGroupService.checkToken(group))) {
//                    throw new ServiceException("文档token失效或者无效");
//                }
                BaseDoc baseDoc = new BaseDoc();
                baseDoc.setDocGroupId(group.getGroupId());
                baseDoc.setDocGroup(group.getGroupPath());
                return baseDocService.uploadDoc(file, baseDoc, request).getDocPath();
            }
        });
    }


}
