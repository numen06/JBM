package com.jbm.util.db.sqltemplate.script;

import java.util.List;

import com.jbm.util.db.sqltemplate.Context;

/**
 * 
 * @author Wesley
 *
 */
public class MixedSqlFragment implements SqlFragment {
	
	private List<SqlFragment> contents ;
	
	public MixedSqlFragment(List<SqlFragment> contents){
		this.contents  = contents ;
	}

	public boolean apply(Context context) {
		
		for(SqlFragment sf : contents){
			sf.apply(context);
		}
		
		return true;
	}
	
	
	
	

}
