package com.jbm.util.db.sqltemplate.script;

import java.util.Arrays;
import java.util.List;

/**
 * 
 * @author Wesley
 * 
 */
public class SetFragment extends TrimFragment {

	private static List<String> suffixList = Arrays.asList(",");

	public SetFragment(SqlFragment contents) {
		super(contents, "SET", null, null, suffixList);
	}

}
