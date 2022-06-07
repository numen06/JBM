package com.jbm.cluster.api.mapstruct;

import com.jbm.cluster.api.entitys.basic.BaseAccount;
import com.jbm.cluster.api.form.auth.PasswordLoginWay;
import com.jbm.cluster.api.form.auth.SmsLoginWay;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * @Created wesley.zhang
 * @Date 2022/6/2 15:37
 * @Description TODO
 */
@Mapper
public interface LoginAccountStruct {

    LoginAccountStruct INSTANCT = Mappers.getMapper(LoginAccountStruct.class);

//    @Mapping(target = "account", source = "username")
        // 忽略id，不进行映射
    BaseAccount converLoginWay(PasswordLoginWay passwordLoginWay);

//    @Mapping(target = "account", source = "mobile")
        // 忽略id，不进行映射
    BaseAccount converLoginWay(SmsLoginWay smsLoginAccount);
}
