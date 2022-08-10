package com.jbm.framework.service.mybatis;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.google.common.collect.Maps;
import com.jbm.framework.masterdata.mapper.SuperMapper;
import com.jbm.framework.masterdata.usage.CriteriaQueryWrapper;
import com.jbm.framework.masterdata.usage.PageParams;
import com.jbm.framework.masterdata.usage.bean.EntityMap;
import com.jbm.framework.masterdata.utils.ServiceUtils;
import com.jbm.framework.usage.paging.DataPaging;
import com.jbm.framework.usage.paging.PageForm;
import org.apache.ibatis.annotations.Param;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * @author: wesley.zhang
 * @date: 2018/12/24 12:49
 * @desc: 父类service
 */
public abstract class BaseServiceImpl<M extends SuperMapper<T>, T> extends ServiceImpl<M, T> {

    @Autowired
    public ApplicationContext applicationContext;
    @Resource
    public SqlSessionTemplate sqlSession;


    public IPage pageList(CriteriaQueryWrapper<?> wrapper) {
        PageParams page = wrapper.getPageParams();
        IPage list = this.baseMapper.pageList(page, wrapper);
        EntityMap.setEnumConvertInterceptor(null);
        return list;
    }

    public EntityMap getEntityMap(CriteriaQueryWrapper<?> cq) {
        List<EntityMap> maps = baseMapper.getEntityMap(cq);
        if (ObjectUtils.isEmpty(maps)) {
            //避免空对象输出""
            return new EntityMap();
        }
        return maps.get(0);
    }

    public List<EntityMap> listEntityMaps(CriteriaQueryWrapper<?> cq) {
        List<EntityMap> map = baseMapper.getEntityMap(cq);
        return map;
    }

    /**
     * 自定义sql查询List<EntityMap>
     */
    public List<EntityMap> selectListEntityMap(String statement, EntityMap map) {
        if (ObjectUtils.isEmpty(map)) {
            return null;
        }
        return sqlSession.selectList(getMapperName() + statement, map);
    }

    /**
     * 自定义sql查询List<EntityMap>
     */
    public List<EntityMap> selectListEntityMap(EntityMap map) {
        if (ObjectUtils.isEmpty(map)) {
            return null;
        }
        return sqlSession.selectList(getMapperName() + "selectListEntityMapByMap", map);
    }

    /**
     * 自定义sql查询List<EntityMap>
     */
    public List<EntityMap> selectListEntityMap(String statement, @Param("ew") CriteriaQueryWrapper<?> cq) {
        return sqlSession.selectList(getMapperName() + statement, cq);
    }

    /**
     * 自定义sql查询List<EntityMap>
     */
    public List<EntityMap> selectListEntityMap(@Param("ew") CriteriaQueryWrapper<?> cq) {
        return sqlSession.selectList(getMapperName() + "selectListEntityMapByCq", cq);
    }


    public <T, P extends Page<T>> DataPaging<T> selectPageList(PageForm pageForm, Consumer<Page<T>> function) {
        Page<T> page = ServiceUtils.buildPage(pageForm);
        function.accept(page);
        DataPaging<T> dataPaging = ServiceUtils.pageToDataPaging(page, pageForm);
        return dataPaging;
    }


    /**
     * 根据当前MAPPER的方法名称执行分页查询
     *
     * @param statement
     * @param obj
     * @param pageForm
     * @param <T>
     * @return
     */
    protected <T> DataPaging<T> selectMapperPaging(final String statement, Object obj, PageForm pageForm) {
        return this.selectMapperPaging(baseMapper.getClass(), statement, obj, pageForm);
    }

    /**
     * 根据对应Mapper的方法名称执行分页查询
     *
     * @param mapper
     * @param statement
     * @param obj
     * @param pageForm
     * @param <T>
     * @return
     */
    protected <T> DataPaging<T> selectMapperPaging(final Class mapper, final String statement, Object obj, PageForm pageForm) {
        if (!ClassUtil.getPublicMethodNames(mapper).contains(statement)) {
            throw new NullPointerException("不存在对应的查询方法");
        }
        final Page<Map<String, Object>> page = ServiceUtils.buildPage(pageForm);
        final Map<String, Object> tempParams = ObjectUtil.isEmpty(obj) ? Maps.newLinkedHashMap() : obj instanceof Map ? (Map<String, Object>) obj : BeanUtil.beanToMap(obj);
        tempParams.put(UUID.randomUUID().toString(), page);
        final List<T> list = sqlSession.selectList(sqlStatement(mapper, statement), tempParams);
        final DataPaging<T> dataPaging = new DataPaging<>(list, page.getTotal(), page.getPages(), pageForm);
        return dataPaging;
    }


    /**
     * 获取mapperName
     */
    public String getMapperName() {
        return getMapperName(baseMapper.getClass());
    }

    public String getMapperName(Class mapper) {
        String mapperName = "";
        Class<?> interfaces[] = mapper.getInterfaces();
        for (Class<?> anInterface : interfaces) {
            mapperName = anInterface.getName();
        }
        return mapperName + ".";
    }

    public List<T> selectAll() {
        return this.baseMapper.selectList(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(Collection<T> entityList) {
        return super.saveBatch(entityList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByMap(Map<String, Object> columnMap) {
        return super.removeByMap(columnMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(T entity) {
        return super.updateById(entity);
    }

    protected String sqlStatement(String sqlMethod) {
        return SqlHelper.table(entityClass).getSqlStatement(sqlMethod);
    }

    protected String sqlStatement(Class mapper, String sqlMethod) {
        return StrUtil.concat(true, getMapperName(mapper), sqlMethod);
    }
}
