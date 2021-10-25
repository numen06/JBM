package com.jbm.framework.service.mybatis;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jbm.framework.masterdata.mapper.SuperMapper;
import com.jbm.framework.masterdata.usage.CriteriaQueryWrapper;
import com.jbm.framework.masterdata.usage.PageParams;
import com.jbm.framework.masterdata.usage.bean.EntityMap;
import org.apache.ibatis.annotations.Param;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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

    @Override
    public boolean removeById(Serializable id) {
        return super.removeById(id);
    }

    @Override
    public boolean removeByMap(Map<String, Object> columnMap) {
        return super.removeByMap(columnMap);
    }

    @Override
    public boolean removeByIds(Collection<? extends Serializable> idList) {
        return super.removeByIds(idList);
    }

    @Override
    public boolean updateById(T entity) {
        return super.updateById(entity);
    }

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

    /**
     * 获取mapperName
     */
    public String getMapperName() {
        String mapperName = "";
        Class cl = baseMapper.getClass();
        Class<?> interfaces[] = cl.getInterfaces();
        for (Class<?> anInterface : interfaces) {
            mapperName = anInterface.getName();
        }
        return mapperName + ".";
    }

    public List<T> selectAll() {
        return this.baseMapper.selectList(null);
    }


}
