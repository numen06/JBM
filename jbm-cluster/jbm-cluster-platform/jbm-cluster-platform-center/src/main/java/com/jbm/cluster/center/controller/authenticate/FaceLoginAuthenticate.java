package com.jbm.cluster.center.controller.authenticate;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baidu.aip.face.AipFace;
import com.baidu.aip.face.MatchRequest;
import com.google.common.collect.Lists;
import com.jbm.cluster.api.constants.AccountType;
import com.jbm.cluster.api.constants.LoginType;
import com.jbm.cluster.api.entitys.basic.BaseUserCertification;
import com.jbm.cluster.api.model.auth.JbmLoginUser;
import com.jbm.cluster.api.service.ILoginAuthenticate;
import com.jbm.cluster.center.service.BaseUserCertificationService;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.metadata.bean.ResultBody;
import jbm.framework.boot.autoconfigure.baidu.model.BaiduResult;
import jbm.framework.boot.autoconfigure.baidu.model.result.MatchResult;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 人脸识别登录
 */
@Service
public class FaceLoginAuthenticate implements ILoginAuthenticate {

    @Autowired
    private LoginAuthenticateHelper loginAuthenticateHelper;
    @Autowired(required = false)
    private AipFace aipFace;
    @Autowired
    private BaseUserCertificationService baseUserCertificationService;

    @Override
    public ResultBody<JbmLoginUser> login(String username, String password, String loginType) {
        return ResultBody.callback("人脸登录成功", new Supplier<JbmLoginUser>() {
            @SneakyThrows
            @Override
            public JbmLoginUser get() {
                Validator.validateMobile(username, "非法手机号");
                final String phone = username;
                JbmLoginUser jbmLoginUser = loginAuthenticateHelper.loginByAccount(phone, AccountType.mobile);
                try {
                    BaseUserCertification baseUserCertification = baseUserCertificationService.findByUserId(jbmLoginUser.getUserId());
                    if (ObjectUtil.isEmpty(baseUserCertification)) {
                        throw new ServiceException("该用户没有注册实名信息");
                    }
                    String image = baseUserCertification.getFaceImage();
                    MatchRequest matchRequest1 = new MatchRequest(StrUtil.subAfter(image, "base64,", false), "BASE64");
                    MatchRequest matchRequest2 = new MatchRequest(StrUtil.subAfter(password, "base64,", false), "BASE64");
                    JSONObject jsonObject = aipFace.match(Lists.newArrayList(matchRequest1, matchRequest2));
                    BaiduResult<MatchResult> baiduResult = JSON.parseObject(jsonObject.toString(), new TypeReference<BaiduResult<MatchResult>>() {
                    });
                    final AtomicBoolean success = new AtomicBoolean(false);
                    baiduResult.successAction(new Consumer<MatchResult>() {
                        @Override
                        public void accept(MatchResult matchResult) {
                            if (baiduResult.getResult().getScore() > 92.0) {
                                success.set(true);
                            } else {

                                throw new ServiceException("人脸不匹配");
                            }
                        }
                    });
                    if (success.get()) {
                        return jbmLoginUser;
                    } else {
                        throw new ServiceException("人脸识别失败");
                    }
                } catch (Exception e) {
                    throw new ServiceException(e);
                }
            }
        });
    }

    @Override
    public List<LoginType> getLoginType() {
        return Lists.newArrayList(LoginType.FACE);
    }
}
