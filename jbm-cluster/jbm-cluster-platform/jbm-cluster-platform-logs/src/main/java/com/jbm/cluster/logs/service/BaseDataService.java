package com.jbm.cluster.logs.service;

import java.util.List;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2021-05-06 17:01
 **/
public interface BaseDataService<Entity> {

    long count();

    Entity save(Entity commodity);

    void delete(Entity commodity);

    List<Entity> getAll();

}
