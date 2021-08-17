package com.jbm.cluster.logs.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jbm.cluster.logs.service.BaseDataService;
import com.jbm.framework.usage.paging.PageForm;
import com.jbm.util.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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

    public Criteria likeCriteria(String key, String value) {
        String regex = StrUtil.concat(true, "^.*", StrUtil.emptyToDefault(value, ""), ".*$");
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        return Criteria.where(key).regex(pattern);
    }

    /**
     * 增加排序
     *
     * @param pageable
     * @param order
     * @return
     */
    public PageRequest addOrder(PageRequest pageable, Sort.Order order) {
        List<Sort.Order> orders = Lists.newArrayList(pageable.getSort().iterator());
        orders.add(order);
        pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));
        return pageable;
    }

    public PageRequest toPageRequest(PageForm pageForm, Sort.Order... defOrders) {
        if (pageForm == null) {
            pageForm = PageForm.NO_PAGING();
        }
        if (ObjectUtil.isEmpty(pageForm.getCurrPage())) {
            pageForm = PageForm.NO_PAGING();
        }
        List<Sort.Order> orders = Lists.newArrayList(defOrders);
        final Map<String, String> rule = MapUtils.split(pageForm.getSortRule(), Maps.newLinkedHashMap(), ",", ":");
        for (String col : rule.keySet()) {
            String sort = rule.get(col);
            if (Sort.Direction.DESC.toString().equalsIgnoreCase(sort)) {
                orders.add(Sort.Order.desc(col));
            } else {
                orders.add(Sort.Order.asc(col));
            }
        }
        PageRequest pageable = PageRequest.of(pageForm.getCurrPage() - 1, pageForm.getPageSize(), Sort.by(orders));
        return pageable;
    }

}
