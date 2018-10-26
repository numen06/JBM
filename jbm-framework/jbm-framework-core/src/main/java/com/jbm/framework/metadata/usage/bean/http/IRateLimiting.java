package com.jbm.framework.metadata.usage.bean.http;

public interface IRateLimiting {
	public int getRateLimitQuota();

	public int getRateLimitRemaining();

	public int getRateLimitReset();
}
