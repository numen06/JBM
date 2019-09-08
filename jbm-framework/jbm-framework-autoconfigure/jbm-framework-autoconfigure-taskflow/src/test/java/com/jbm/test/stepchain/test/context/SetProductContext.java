package com.jbm.test.stepchain.test.context;

import com.github.zengfr.project.stepchain.context.DataContext;
import com.jbm.test.stepchain.test.SetProductRequest;
import com.jbm.test.stepchain.test.SetProductResponse;

public class SetProductContext extends DataContext<SetProductRequest, SetProductDataMiddle, SetProductResponse> {

	public SetProductContext(SetProductRequest a, SetProductDataMiddle b, SetProductResponse c) {
		super(a, b, c);
	}
	
}
