package jbm.framework.boot.autoconfigure.mysql;

/**
 * 默认为InnoDB和utf8mb4模式
 *
 * @author wesley
 */
public class MySQL5TableType extends org.hibernate.dialect.MySQL5Dialect {

    @Override
    public String getTableTypeString() {
        return "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
    }

}
