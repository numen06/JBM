package com.jbm.cluster.center.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baidu.aip.face.AipFace;
import com.jbm.cluster.api.entitys.basic.BaseUserCertification;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.center.service.BaseUserCertificationService;
import com.jbm.cluster.common.satoken.utils.LoginHelper;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataCollection;
import com.jbm.framework.usage.form.JsonRequestBody;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jbm.framework.boot.autoconfigure.baidu.model.BaiduResult;
import jbm.framework.boot.autoconfigure.baidu.model.result.MatchResult;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Author: wesley.zhang
 * @Create: 2022-07-19 14:01:27
 */
@Api(tags = "用户实名认证开放接口")
@RestController
@RequestMapping("/baseUserCertification")
public class BaseUserCertificationController extends MasterDataCollection<BaseUserCertification, BaseUserCertificationService> {

    @Autowired(required = false)
    private AipFace aipFace;

    @ApiOperation(value = "上传人脸信息")
    @PostMapping("/updateFaceImage")
    public ResultBody<MatchResult> updateFaceImage(@RequestBody JsonRequestBody jsonRequestBody) {
        return ResultBody.callback("人脸检测成功", () -> {
            String base64 = jsonRequestBody.getString("faceImage");
            String faceImage = StrUtil.subAfter(base64, "base64,", false);
            if (StrUtil.isBlank(faceImage)) {
                throw new ServiceException("未检测到人脸信息");
            }
            JSONObject jsonObject = aipFace.detect(faceImage, "BASE64", null);
            BaiduResult<MatchResult> baiduResult = JSON.parseObject(jsonObject.toString(), new TypeReference<BaiduResult<MatchResult>>() {
            });
            if (ObjectUtil.isEmpty(baiduResult.getResult())) {
                throw new ServiceException("未检测到人脸信息");
            }
            JbmLoginUser jbmLoginUser = LoginHelper.getLoginUser();
            BaseUserCertification baseUserCertification = service.findByUserId(jbmLoginUser.getUserId());
            if (ObjectUtil.isEmpty(baseUserCertification)) {
                baseUserCertification = new BaseUserCertification();
            }
            baseUserCertification.setUserId(jbmLoginUser.getUserId());
            baseUserCertification.setFaceImage(faceImage);
            service.saveEntity(baseUserCertification);
            return baiduResult.getResult();
        });
    }

    @ApiOperation(value = "当前用户认证信息")
    @GetMapping("/currentUserCert")
    public ResultBody<BaseUserCertification> currentUserCert() {
        return ResultBody.callback(() -> {
            JbmLoginUser jbmLoginUser = LoginHelper.getLoginUser();
            BaseUserCertification baseUserCertification = service.findByUserId(jbmLoginUser.getUserId());
            return baseUserCertification;
        });
    }
}

