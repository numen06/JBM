package com.jbm.framework.usage.paging;

import com.jbm.framework.metadata.usage.bean.IBaseForm;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <pre>
 * 对数据进行封装的基础类
 * 特别注意PageForm开始页是1
 * </pre>
 *
 * @author wesley
 */
@Data
@ApiModel(value = "分页实体")
public class PageForm implements IBaseForm {

    private static final long serialVersionUID = 1L;

    public final static PageForm NO_PAGING() {
        return new PageForm(1, Integer.MAX_VALUE);
    }

    /**
     * 当前页
     */
    @ApiModelProperty(value = "当前页")
    private Integer currPage;
    /**
     * 单页数量
     */
    @ApiModelProperty(value = "单页数量")
    private Integer pageSize;
    /**
     * 排序字段组合<br/>
     * id:asc,sort:desc
     */
    @ApiModelProperty(value = "排序规则—id:asc,code:desc")
    private String sortRule;

    /**
     * 搜索词
     */
    @ApiModelProperty(value = "搜索词")
    private String keyword;

    /**
     * 匹配规则<br/>
     * field:eq,field:in,field:like
     */
    @ApiModelProperty(value = "匹配规则")
    private MatchRule matchRule;

    public PageForm() {
        this(1, Integer.MAX_VALUE);
    }

    public PageForm(Integer currPage, Integer pageSize) {
        super();
        this.currPage = currPage;
        this.pageSize = pageSize;
    }

    public PageForm(Integer currPage, Integer pageSize, String sortRule) {
        super();
        this.currPage = currPage;
        this.pageSize = pageSize;
        this.sortRule = sortRule;
    }

    public void setPage(Integer page) {
        this.currPage = page;
    }

    public void setLimit(Integer limit) {
        this.pageSize = limit;
    }


}
