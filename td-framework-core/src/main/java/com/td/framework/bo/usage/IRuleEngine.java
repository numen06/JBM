package com.td.framework.bo.usage;

import com.td.framework.metadata.exceptions.BizRuleException;

public interface IRuleEngine<A extends IBizAction> {

	void initEngine() throws BizRuleException;

	void extRule(String rulekey, Object... args) throws BizRuleException;

}
