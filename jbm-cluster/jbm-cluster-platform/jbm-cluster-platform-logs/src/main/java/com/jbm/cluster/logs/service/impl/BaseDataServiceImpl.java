package com.jbm.cluster.logs.service.impl;

import com.jbm.cluster.logs.service.BaseDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2021-05-06 17:03
 **/
public class BaseDataServiceImpl<Entity, Repository extends ElasticsearchRepository<Entity, String>> implements BaseDataService<Entity> {

    @Autowired
    private Repository repository;

    @Override
    public long count() {
        return repository.count();
    }


    @Override
    public Entity save(Entity commodity) {
        return repository.save(commodity);
    }

    @Override
    public void delete(Entity commodity) {
        repository.delete(commodity);
//        commodityRepository.deleteById(commodity.getSkuId());
    }

    @Override
    public Iterable<Entity> getAll() {
        return repository.findAll();
    }


}
