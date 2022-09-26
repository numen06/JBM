package com.jbm.cluster.auth.service.authenticate;

import com.google.common.collect.Lists;
import com.jbm.cluster.api.constants.LoginType;
import com.jbm.cluster.api.service.ILoginAuthenticate;
import com.jbm.framework.metadata.bean.ResultBody;

import java.util.List;

/**
 * @Created wesley.zhang
 * @Date 2022/5/19 15:23
 * @Description TODO
 */
public class UserNameLoginAuthenticate implements ILoginAuthenticate {

    @Override
    public ResultBody login(String username, String password, String loginType) {
        return null;
    }

    @Override
    public List<LoginType> getLoginType() {
        return Lists.newArrayList(LoginType.PASSWORD);
    }
}
