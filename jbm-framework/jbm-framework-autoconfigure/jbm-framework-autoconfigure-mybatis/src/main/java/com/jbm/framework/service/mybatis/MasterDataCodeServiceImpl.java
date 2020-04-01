package com.jbm.framework.service.mybatis;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
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

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
    private boolean updateBatchByCode(Collection<Entity> entityList, int batchSize) {
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
    private boolean saveOrUpdateBatchByCode(Collection<Entity> entityList, int batchSize) {
        if (CollectionUtils.isEmpty(entityList)) {
            throw new IllegalArgumentException("Error: entityList must not be empty");
        }
        Class<?> cls = null;
        TableInfo tableInfo = null;
        int i = 0;
        try (SqlSession batchSqlSession = sqlSessionBatch()) {
            for (Entity anEntityList : entityList) {
                if (i == 0) {
                    cls = anEntityList.getClass();
                    tableInfo = TableInfoHelper.getTableInfo(cls);
                }
                if (null != tableInfo && StringUtils.isNotEmpty(tableInfo.getKeyProperty())) {
                    Object codeVal = ReflectionKit.getMethodValue(cls, anEntityList, IMasterDataCodeService.CODE_COLUMN);
                    if (StringUtils.checkValNull(codeVal)) {
                        String sqlStatement = sqlStatement(SqlMethod.INSERT_ONE);
                        batchSqlSession.insert(sqlStatement, anEntityList);
                    } else {
                        String sqlStatement = sqlStatement(MasterDataSqlInjector.UPDATE_BY_CODE);
                        MapperMethod.ParamMap<Entity> param = new MapperMethod.ParamMap<>();
                        param.put(Constants.ENTITY, anEntityList);
                        batchSqlSession.update(sqlStatement, param);
                        // 不知道以后会不会有人说更新失败了还要执行插入 😂😂😂
                    }
                    if (i >= 1 && i % batchSize == 0) {
                        batchSqlSession.flushStatements();
                    }
                    i++;
                } else {
                    throw ExceptionUtils.mpe("Error:  Can not execute. Could not find @TableId.");
                }
                batchSqlSession.flushStatements();
            }
        }
        return true;
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
