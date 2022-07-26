package jbm.framework.test;

public class ConTest {

    private int index = 0;

    public ConTest() {
        super();
        System.out.println("我被初始化了");
    }

    public void link() {
        System.out.println("link - " + index++);
    }

    public int getIndex() {
        return index;
    }

}
