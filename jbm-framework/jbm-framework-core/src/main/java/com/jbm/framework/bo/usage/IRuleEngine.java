package com.jbm.framework.bo.usage;

import com.jbm.framework.exceptions.BizRuleException;

public interface IRuleEngine<A extends IBizAction> {

	void initEngine() throws BizRuleException;

	void extRule(String rulekey, Object... args) throws BizRuleException;

}
