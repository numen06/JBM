package com.jbm.util.comparator;

public class IntegerComparator extends AbstractComparator<Integer> {

    public IntegerComparator() {
        super();
    }

    public IntegerComparator(String sort) {
        super(sort);
    }

    @Override
    public int compare(Integer o1, Integer o2) {
        if (DESC.equals(super.getSort())) {
            return o1 < o2 ? 1 : -1;
        } else {
            return o1 > o2 ? 1 : -1;
        }
    }

}
