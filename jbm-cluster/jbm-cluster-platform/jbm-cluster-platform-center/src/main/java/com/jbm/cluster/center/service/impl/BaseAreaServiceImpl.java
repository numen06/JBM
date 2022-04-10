package com.jbm.cluster.center.service.impl;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.pinyin.PinyinUtil;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.jbm.cluster.api.constants.AreaType;
import com.jbm.cluster.api.model.entity.BaseArea;
import com.jbm.cluster.center.mapper.BaseAreaMapper;
import com.jbm.cluster.center.service.BaseAreaService;
import com.jbm.framework.service.mybatis.MasterDataServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;

/**
 * @Author: auto generate by jbm
 * @Create: 2022-04-07 21:44:18
 */
@Slf4j
@Service
public class BaseAreaServiceImpl extends MasterDataServiceImpl<BaseArea> implements BaseAreaService, ApplicationListener<ApplicationReadyEvent> {

    private final static String CACHE_KEY = "areaMap";

    @Resource
    private BaseAreaMapper baseAreaMapper;

    @Override
    @Cacheable(value = CACHE_KEY)
    public Map<String, List<BaseArea>> getChinaAreaMap() {
        List<BaseArea> listRoot = baseAreaMapper.selectByParentCode("86");
        Map<String, List<BaseArea>> result = Maps.newLinkedHashMap();
        for (int i = 0; i < listRoot.size(); i++) {
            String code = listRoot.get(i).getAreaCode();
            List<BaseArea> listDic = baseAreaMapper.selectByParentCode(code);
            result.put(code, listDic);
        }
        return result;
    }


    @Override
    @CacheEvict(value = CACHE_KEY, allEntries = true)
    public BaseArea saveEntity(BaseArea entity) {
        return super.saveEntity(entity);
    }


    private void read(Map<String, BaseArea> baseAreaMap, JSONObject jsonObject, BaseArea parentBaseArea) {
        JSONObject sub = jsonObject.getJSONObject(parentBaseArea.getAreaCode());
        if (MapUtil.isEmpty(sub))
            return;
        for (String key : sub.keySet()) {
            BaseArea baseArea = new BaseArea();
            baseArea.setAreaCode(key);
            if (ObjectUtil.isNotEmpty(parentBaseArea)) {
                baseArea.setParentCode(parentBaseArea.getAreaCode());
            }
            baseArea.setAreaName(sub.getString(key));
            String full = PinyinUtil.getPinyin(baseArea.getAreaName());
            baseArea.setFullPinYin(full);
            baseArea.setSimplePinYin(PinyinUtil.getFirstLetter(baseArea.getAreaName(), " "));
            this.buildAreaType(baseArea, parentBaseArea);
            baseAreaMap.put(baseArea.getAreaCode(), baseArea);
            //去地鬼监测是否还存下级
            this.read(baseAreaMap, jsonObject, baseArea);
        }
    }

    /**
     * 系统启动校验数据
     *
     * @param event
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        //存在之后不初始化
        if (this.count() > 0) {
            return;
        }
        Map<String, BaseArea> baseAreaMap = Maps.newHashMap();
        try {
            File jsonFile = ResourceUtils.getFile("classpath:data/area.json");
            String dataStr = IoUtil.readUtf8(new FileInputStream(jsonFile));
            JSONObject jsonObject = JSONObject.parseObject(dataStr);
            BaseArea rootArea = new BaseArea();
            rootArea.setAreaCode("86");
            rootArea.setAreaName("中国");
            rootArea.setAreaType(AreaType.country);
            String full = PinyinUtil.getPinyin(rootArea.getAreaName());
            rootArea.setFullPinYin(full);
            rootArea.setSimplePinYin(PinyinUtil.getFirstLetter(rootArea.getAreaName(), " "));
            baseAreaMap.put(rootArea.getAreaCode(), rootArea);
            this.read(baseAreaMap, jsonObject, rootArea);
        } catch (Exception ex) {
            log.error("读取文件并注入区域错误");
        } finally {
            if (MapUtil.isNotEmpty(baseAreaMap))
                this.saveEntitys(baseAreaMap.values());
        }
    }

    private void buildAreaType(BaseArea baseArea, BaseArea parentBaseArea) {
        if (ObjectUtil.isEmpty(parentBaseArea) || ObjectUtil.isEmpty(parentBaseArea.getAreaType())) {
            baseArea.setAreaType(AreaType.country);
            return;
        } else {
            switch (parentBaseArea.getAreaType()) {
                case country:
                    baseArea.setAreaType(AreaType.province);
                    break;
                case province:
                    baseArea.setAreaType(AreaType.city);
                    break;
                case city:
                    baseArea.setAreaType(AreaType.district);
                    break;
            }
        }
    }
}