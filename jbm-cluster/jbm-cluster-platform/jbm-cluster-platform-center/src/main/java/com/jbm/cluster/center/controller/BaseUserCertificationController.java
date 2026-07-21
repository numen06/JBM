package com.jbm.cluster.center.controller;

import java.util.List;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.masterdata.usage.form.MasterDataRequsetBody;
import com.jbm.framework.form.IdsForm;
import com.jbm.cluster.core.constant.JbmConstants;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
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
import jbm.framework.boot.autoconfigure.baidu.model.result.DetectResult;
import jbm.framework.boot.autoconfigure.baidu.model.result.FaceInfo;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

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
    public ResultBody<DetectResult> updateFaceImage(@RequestBody JsonRequestBody jsonRequestBody) {
        return ResultBody.callback("人脸检测成功", () -> {
            String base64 = jsonRequestBody.getString("faceImage");

            String faceImage = StrUtil.subAfter(base64, "base64,", false);
            if (StrUtil.isBlank(faceImage)) {
                throw new ServiceException("未检测到人脸信息");
            }
            HashMap<String, Object> options = new HashMap<String, Object>();
            options.put("face_field", "age");
            options.put("max_face_num", "2");
            options.put("face_type", "LIVE");
            options.put("liveness_control", "LOW");
            JSONObject jsonObject = aipFace.detect(faceImage, "BASE64", options);
            BaiduResult<DetectResult> baiduResult = JSON.parseObject(jsonObject.toString(), new TypeReference<BaiduResult<DetectResult>>() {
            });
            if (ObjectUtil.isEmpty(baiduResult.getResult())) {
                throw new ServiceException("未检测到人脸信息");
            }
            if (baiduResult.getResult().getFaceNum() == 0) {
                throw new ServiceException("未检测到人脸信息");
            }
            if (baiduResult.getResult().getFaceNum() > 1) {
                throw new ServiceException("检测到多张人脸");
            }
            FaceInfo faceInfo = CollUtil.getFirst(baiduResult.getResult().getFaceList());
            if (!NumberUtil.equals(faceInfo.getFaceProbability().doubleValue(), 1.0)) {
                throw new ServiceException("人脸可信度差请重新上传");
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

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "获取分页列表", notes = "获取分页列表")
    @PostMapping("/pageList")
    @Override
    public ResultBody<DataPaging<BaseUserCertification>> pageList(@RequestBody(required = false) PageRequestBody pageRequestBody) {
        return super.pageList(pageRequestBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "获取列表", notes = "获取列表")
    @PostMapping("/list")
    @Override
    public ResultBody<List<BaseUserCertification>> list(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        return super.list(masterDataRequsetBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "获取单个实体", notes = "获取单个实体")
    @PostMapping("/model")
    @Override
    public ResultBody<BaseUserCertification> model(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        return super.model(masterDataRequsetBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "保存单个实体", notes = "保存单个实体")
    @PostMapping("/save")
    @Override
    public ResultBody<BaseUserCertification> save(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        return super.save(masterDataRequsetBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "批量保存", notes = "批量保存")
    @PostMapping("/saveBatch")
    @Override
    public ResultBody<List<BaseUserCertification>> saveBatch(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        return super.saveBatch(masterDataRequsetBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "模拟数据", notes = "模拟数据")
    @PostMapping("/mock")
    @Override
    public ResultBody<BaseUserCertification> mock() {
        return super.mock();
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "删除实体", notes = "删除实体")
    @PostMapping("/delete")
    @Override
    public ResultBody<Boolean> remove(@RequestBody(required = false) MasterDataRequsetBody masterDataRequsetBody) {
        return super.remove(masterDataRequsetBody);
    }

    @SaCheckRole(JbmConstants.USER_TYPE_ADMIN)
    @ApiOperation(value = "通过id删除实体", notes = "通过id删除实体")
    @PostMapping("/deleteByIds")
    @Override
    public ResultBody<Boolean> deleteByIds(@RequestBody(required = false) IdsForm idsForm) {
        return super.deleteByIds(idsForm);
    }

}

