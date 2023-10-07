package com.jbm.util.comparator;

public class DoubleComparator extends AbstractComparator<Double> {

    public DoubleComparator() {
        super();
    }

    public DoubleComparator(String sort) {
        super(sort);
    }

    @Override
    public int compare(Double o1, Double o2) {
        if (DESC.equals(super.getSort())) {
            return o1 < o2 ? 1 : -1;
        } else {
            return o1 > o2 ? 1 : -1;
        }
    }

}
