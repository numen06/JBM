package com.jbm.framework.test.bean;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author: create by wesley
 * @date:2019/5/2
 */
@Data
public class SelfRefData {
    private Integer id;
    private String name;
    private SelfRefData parent;
    private Map<SelfRefData, SelfRefData> parentMap;
    private SelfRefData[] parentArray;
    private List<SelfRefData> list;
    private List<SelfRefData[]> listArray;
    private List<List<SelfRefData[]>> listListArray;
    private List<SelfRefData>[] arrayList;

    private SelfRefData[][][] data;
    private Map<SelfRefData, SelfRefData[]> mapArray;
    private Map<SelfRefData, List<SelfRefData>> mapList;
    private Map<SelfRefData, List<SelfRefData[]>> mapListArray;
}
