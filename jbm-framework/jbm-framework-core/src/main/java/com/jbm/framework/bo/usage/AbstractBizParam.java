package com.jbm.framework.bo.usage;

public abstract class AbstractBizParam implements IBizParam {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 流程ID
	 */
	private Long processId;

	/**
	 * 展示层ID
	 */
	private Long pageId;

	public Long getProcessId() {
		return processId;
	}

	public void setProcessId(Long processId) {
		this.processId = processId;
	}

	public Long getPageId() {
		return pageId;
	}

	public void setPageId(Long pageId) {
		this.pageId = pageId;
	}

}
