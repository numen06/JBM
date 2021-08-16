package com.jbm.cluster.logs.service.impl;

import com.jbm.cluster.logs.service.BaseDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2021-05-06 17:03
 **/
public class BaseDataServiceImpl<Entity, Repository extends MongoRepository<Entity, String>> implements BaseDataService<Entity> {

    @Autowired
    private Repository repository;

    @Autowired
    protected MongoTemplate mongoTemplate;

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
    public List<Entity> getAll() {
        return repository.findAll();
    }

}
