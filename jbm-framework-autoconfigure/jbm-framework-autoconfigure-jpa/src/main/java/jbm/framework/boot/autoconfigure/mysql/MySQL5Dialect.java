package jbm.framework.boot.autoconfigure.mysql;

/**
 * 方言修改
 * 
 * @author wesley
 *
 */
public class MySQL5Dialect extends org.hibernate.dialect.MySQL5Dialect {

	@Override
	public String getAddForeignKeyConstraintString(String constraintName, String[] foreignKey, String referencedTable, String[] primaryKey, boolean referencesPrimaryKey) {
		// return super.getAddForeignKeyConstraintString(constraintName,
		// foreignKey, referencedTable, primaryKey, referencesPrimaryKey);
		return " ";
	}

}
