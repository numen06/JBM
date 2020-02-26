package com.jbm.framework.usage.paging;

import cn.hutool.db.Page;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 封装分页数据的封装类
 *
 * @param <E>
 * @author Wesley
 */
@Data
@ApiModel(value = "分页实体")
public class DataPaging<E> implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 空的数据
     */
    public final static List EMPTY_CONTENT = new ArrayList<>();
    /**
     * 空分页对象
     */
    public final static PageForm EMPTY_PAGEABLE = new PageForm();

    /**
     * 查询的内容
     */
    @ApiModelProperty(value = "分页列表")
    private List<E> contents = EMPTY_CONTENT;

    @ApiModelProperty(value = "总条目数")
    private Long total;

    @ApiModelProperty(value = "总页数")
    private Long totalPage;

    /**
     * 查询的分页信息
     */
    private PageForm pageForm = EMPTY_PAGEABLE;

    public DataPaging() {
        super();
    }

    public DataPaging(List<E> contents, DataPaging dataPaging) {
        super();
        this.contents = contents;
        this.total = dataPaging.getTotal();
        this.pageForm = dataPaging.getPageForm();
    }


    public DataPaging(List<E> contents, Long total) {
        super();
        this.contents = contents;
        this.total = total;
    }

    public DataPaging(List<E> contents, Long total, Long totalPage, PageForm pageForm) {
        super();
        this.contents = contents;
        this.total = total;
        this.totalPage = totalPage;
        this.pageForm = pageForm;
    }

    public DataPaging(List<E> contents, Long total, PageForm pageForm) {
        super();
        this.contents = contents;
        this.total = total;
        this.pageForm = pageForm;
    }

    @ApiModelProperty(value = "列表扩展解析")
    private Map<String, Object> exposition;


    public <K, V, T extends Map<K, V>> void putExp(Map<String, T> exp) {
        if (exposition == null)
            exposition = new HashMap<>();
        exp.putAll(exp);
    }

    public <K, V, T extends Map<K, V>> void putExp(String type, T mapper) {
        if (exposition == null)
            exposition = new HashMap<>();
        exposition.put(type, mapper);
    }

    @ApiModelProperty(value = "总条目数")
    public Integer getSize() {
        return pageForm.getPageSize();
    }

    public void setSize(Integer size) {
        pageForm.setPageSize(size.intValue());
    }

    @ApiModelProperty(value = "当前页")
    public Integer getCurrent() {
        return pageForm.getCurrPage();
    }

    public void setCurrent(Integer current) {
        pageForm.setCurrPage(current.intValue());
    }

    @ApiModelProperty(value = "总页数")
    public Long getPages() {
        return totalPage;
    }

    public void setPages(Long pages) {
        this.totalPage = pages;
    }


}
