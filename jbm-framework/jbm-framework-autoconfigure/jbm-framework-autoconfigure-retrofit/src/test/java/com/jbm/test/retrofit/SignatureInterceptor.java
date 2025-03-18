package com.jbm.test.retrofit;

import cn.hutool.core.collection.ListUtil;
import com.jbm.framework.boot.autoconfigure.retrofit.ApiPlatform;
import com.jbm.framework.boot.autoconfigure.retrofit.BaseStrategy;
import com.jbm.framework.boot.autoconfigure.retrofit.interceptor.AbstractInterceptor;
import com.jbm.framework.boot.autoconfigure.retrofit.signature.SignatureStrategy;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author wesley
 */
@Service
@ApiPlatform(name = "", strategys = {HMACSignatureSignatureStrategy.class})
public class SignatureInterceptor extends AbstractInterceptor {


}
