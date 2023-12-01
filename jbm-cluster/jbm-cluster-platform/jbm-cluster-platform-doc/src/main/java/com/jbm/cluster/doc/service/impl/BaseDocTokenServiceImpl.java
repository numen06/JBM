package com.jbm.cluster.doc.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.entitys.doc.BaseDocToken;
import com.jbm.cluster.doc.service.BaseDocTokenService;
import com.jbm.framework.exceptions.ServiceException;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * @Author: auto generate by jbm
 * @Create: 2023-11-28 17:20:20
 */
@Service
public class BaseDocTokenServiceImpl extends MasterDataServiceImpl<BaseDocToken> implements BaseDocTokenService {


    /**
     * 创建一个BaseDocToken对象
     *
     * @param expirationTime 有效期时间
     * @return BaseDocToken 返回创建的BaseDocToken对象
     * @Override 重写父类方法
     */
    public BaseDocToken createGroupToken(Date expirationTime, String groupId) {
        BaseDocToken baseDocToken = new BaseDocToken();
        if (ObjectUtil.isNull(expirationTime)) {
            expirationTime = DateUtil.offsetDay(DateTime.now(), 1);
        }
        baseDocToken.setDocGroupId(groupId);
        baseDocToken.setExpirationTime(expirationTime);
//        baseDocToken.setTokenKey(IdUtil.fastSimpleUUID());
        return this.saveEntity(baseDocToken);
    }


    /**
     * 重写父类方法createDayToken，用于创建明日的Token
     *
     * @return 返回创建的Token对象
     */
    @Override
    public BaseDocToken createGroupDayToken(String groupId) {
        return this.createGroupToken(DateUtil.offsetDay(DateTime.now(), 1), groupId);
    }


//    @Autowired
//    private RedisService redisService;

    /**
     * 重写父类方法，用于检查令牌是否有效
     *
     * @param tokenKey 令牌关键字
     * @return 如果令牌存在则返回true，否则返回false
     */
    @Override
    public BaseDocToken checkToken(String tokenKey) {
        QueryWrapper<BaseDocToken> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(BaseDocToken::getTokenKey, tokenKey);
        BaseDocToken baseDocToken = this.selectEntityByWapper(queryWrapper);
        return baseDocToken;
    }


}