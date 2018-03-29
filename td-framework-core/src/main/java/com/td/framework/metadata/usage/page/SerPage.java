package com.td.framework.metadata.usage.page;

import com.google.common.collect.Lists;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.io.Serializable;

/**
 * 分页信息的封装
 * 
 * @author wesley
 *
 * @param <E>
 */
public class SerPage<E> implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Iterable<? extends E> content;
	private long total = 0l;
	private PageForm pageForm;
	private transient Page<E> page;

	public SerPage(Iterable<? extends E> content, PageForm pageForm, long total) {
		this.content = content;
		this.pageForm = pageForm;
		this.total = total;
	}

	public SerPage(Iterable<? extends E> content) {
		this.content = content;
	}

	public SerPage(Iterable<? extends E> content, PageForm pageForm, Integer total) {
		this.content = content;
		this.total = total;
		this.pageForm = pageForm;
	}

	public Page<E> page() {
		if(content == null)
			content =  Lists.newArrayList();
		if (this.page == null) {
			if(pageForm == null)
				this.page = new PageImpl<E>(Lists.newArrayList(content), null, total);
			else
				this.page = new PageImpl<E>(Lists.newArrayList(content), pageForm.pageable(), total);
		}
		return this.page;
	}

}
