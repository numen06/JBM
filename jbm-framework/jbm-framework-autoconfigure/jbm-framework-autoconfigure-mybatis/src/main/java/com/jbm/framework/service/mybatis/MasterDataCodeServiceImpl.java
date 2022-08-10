package com.jbm.framework.service.mybatis;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jbm.framework.dao.mybatis.sqlInjector.MasterDataSqlInjector;
import com.jbm.framework.exceptions.DataServiceException;
import com.jbm.framework.masterdata.service.IMasterDataCodeService;
import com.jbm.framework.masterdata.usage.entity.MasterDataCodeEntity;
import com.jbm.framework.masterdata.usage.form.PageRequestBody;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.util.CollectionUtils;
import com.jbm.util.MapUtils;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.session.SqlSession;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

public class MasterDataCodeServiceImpl<Entity extends MasterDataCodeEntity> extends MasterDataServiceImpl<Entity> implements IMasterDataCodeService<Entity> {

    public MasterDataCodeServiceImpl() {
        super();
    }

    @Override
    public DataPaging<Entity> findListPage(PageRequestBody pageRequestBody) {
        Entity query = pageRequestBody.tryGet(super.currentModelClass());
        return this.selectEntitys(query, pageRequestBody.getPageForm());
    }

    @Override
    public Entity selectByCode(String code) throws DataServiceException {
        return super.getOne(buildWrapperByCode(code));
    }

    @Override
    public List<Entity> selectByCodes(Collection<String> codes) throws DataServiceException {
        return super.list(buildWrapperByCodes(codes));
    }

    @Override
    public Map<String, Entity> selectEntityMapByCodes(Collection<String> codes) throws DataServiceException {
        try {
            List<Entity> list = this.selectByCodes(codes);
            return MapUtils.fromList(list, IMasterDataCodeService.CODE_COLUMN, String.class);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return Maps.newHashMap();
        }
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public int deleteByCodes(Collection<String> codes) throws DataServiceException {
        return this.baseMapper.delete(this.buildWrapperByCodes(codes));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean deleteByCode(String code) throws DataServiceException {
        return super.remove(this.buildWrapperByCode(code));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateByCode(Entity entity) {
        return this.updateBatchByCode(Lists.newArrayList(entity));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateBatchByCode(Collection<Entity> entityList) {
        return this.updateBatchByCode(entityList, 30);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateBatchByCode(Collection<Entity> entityList, int batchSize) {
        if (CollectionUtils.isEmpty(entityList)) {
            throw new IllegalArgumentException("Error: entityList must not be empty");
        }
        int i = 0;

        String sqlStatement = sqlStatement(MasterDataSqlInjector.UPDATE_BY_CODE);
        try (SqlSession batchSqlSession = sqlSessionBatch()) {
            for (Entity anEntityList : entityList) {
                MapperMethod.ParamMap<Entity> param = new MapperMethod.ParamMap<>();
                param.put(Constants.ENTITY, anEntityList);
                batchSqlSession.update(sqlStatement, param);
                if (i >= 1 && i % batchSize == 0) {
                    batchSqlSession.flushStatements();
                }
                i++;
            }
            batchSqlSession.flushStatements();
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean saveOrUpdateBatchByCode(Collection<Entity> entityList) {
        return this.saveOrUpdateBatchByCode(entityList, 30);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateBatchByCode(Collection<Entity> entityList, int batchSize) {
        if (CollectionUtils.isEmpty(entityList)) {
            throw new IllegalArgumentException("Error: entityList must not be empty");
        }
        Assert.notEmpty(entityList, "error: entityList must not be empty");
        Class<?> cls = currentModelClass();
        TableInfo tableInfo = TableInfoHelper.getTableInfo(cls);
        Assert.notNull(tableInfo, "error: can not execute. because can not find cache of TableInfo for entity!");
        return super.executeBatch(entityList, batchSize, new BiConsumer<SqlSession, Entity>() {
            @Override
            public void accept(SqlSession sqlSession, Entity entity) {
                Object idVal = ReflectionKit.getFieldValue(entity, "code");
                if (StringUtils.checkValNull(idVal) || Objects.isNull(getById((Serializable) idVal))) {
                    sqlSession.insert(sqlStatement(SqlMethod.INSERT_ONE), entity);
                } else {
                    MapperMethod.ParamMap<Entity> param = new MapperMethod.ParamMap<>();
                    param.put(Constants.ENTITY, entity);
                    sqlSession.update(sqlStatement(MasterDataSqlInjector.UPDATE_BY_CODE), param);
                }
            }
        });
    }

    // ---------------------------------------------------------build转换方法----------------------------------------------------------------------

    private QueryWrapper<Entity> buildWrapperByCode(String code) {
        QueryWrapper<Entity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(IMasterDataCodeService.CODE_COLUMN, code);
        return queryWrapper;
    }

    private QueryWrapper<Entity> buildWrapperByCodes(Collection<String> codes) {
        QueryWrapper<Entity> queryWrapper = new QueryWrapper<>();
        queryWrapper.in(IMasterDataCodeService.CODE_COLUMN, codes);
        return queryWrapper;
    }



}
