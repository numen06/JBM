package com.jbm.cluster.common.mysql.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jbm.cluster.api.entitys.basic.OpenApiDocument;
import com.jbm.cluster.common.mysql.mapper.OpenApiDocumentMapper;
import com.jbm.cluster.common.mysql.service.OpenApiDocumentService;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OpenApiDocumentServiceImpl extends MasterDataServiceImpl<OpenApiDocument> implements OpenApiDocumentService {

    @Autowired
    private OpenApiDocumentMapper openApiDocumentMapper;

    @Override
    public OpenApiDocument getByServiceId(String serviceId) {
        QueryWrapper<OpenApiDocument> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(OpenApiDocument::getServiceId, serviceId);
        return openApiDocumentMapper.selectOne(wrapper);
    }
}
