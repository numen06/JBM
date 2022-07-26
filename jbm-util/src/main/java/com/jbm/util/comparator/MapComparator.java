package com.jbm.util.comparator;

import java.util.Map;

@SuppressWarnings("rawtypes")
public class MapComparator<MAP extends Map> extends AbstractComparator<MAP> {

    private Object key;

    public MapComparator() {
        super();
    }

    public MapComparator(Object key) {
        super();
        this.key = key;
    }

    public MapComparator(Object key, String sort) {
        super(sort);
        this.key = key;
    }

    public Object getKey() {
        return key;
    }

    public void setKey(Object key) {
        this.key = key;
    }

    @Override
    public int compare(MAP o1, MAP o2) {
        // o1，o2是list中的Map，可以在其内取得值，按其排序，此例为降序，s1和s2是排序字段值
        Object v1 = o1.get(key);
        Object v2 = o2.get(key);
        return ComparatorFactory.creatComparator(v1, super.getSort()).compare(v1, v2);
    }

}
