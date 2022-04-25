package com.jbm.test.center;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.pinyin.PinyinUtil;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.jbm.cluster.api.constants.AreaType;
import com.jbm.cluster.api.model.entity.BaseArea;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

public class AreaTest {
//    private Map<String, String> MAP_DATA = Maps.newHashMap();

    @BeforeEach
    public void init() {
        Map<String, BaseArea> baseAreaMap = Maps.newHashMap();
        try {
            File jsonFile = ResourceUtils.getFile("classpath:data/area.json");
            String dataStr = IoUtil.readUtf8(new FileInputStream(jsonFile));
            JSONObject jsonObject = JSONObject.parseObject(dataStr);
            this.readRoot(baseAreaMap, jsonObject);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println(StrUtil.format("行政区域数据{}条", baseAreaMap.size()));
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

    private void readRoot(Map<String, BaseArea> baseAreaMap, JSONObject jsonObject) {
        BaseArea rootArea = new BaseArea();
        rootArea.setAreaCode("86");
        rootArea.setAreaName("中国");
        rootArea.setAreaType(AreaType.country);
        this.read(baseAreaMap, jsonObject, rootArea);
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


//    private void read(JSONObject jsonObject, int level) {
//        for (String key : jsonObject.keySet()) {
//            Object val = jsonObject.get(key);
//            if (val instanceof JSONObject) {
//                this.read(jsonObject.getJSONObject(key), level++);
//            } else {
//                MAP_DATA.put(key, jsonObject.getString(key));
//            }
//        }
//    }

    @Test
    public void test() {
    }


}
