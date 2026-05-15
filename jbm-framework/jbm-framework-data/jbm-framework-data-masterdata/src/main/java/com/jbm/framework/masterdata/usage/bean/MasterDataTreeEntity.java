package com.jbm.framework.masterdata.usage.bean;

import com.baomidou.mybatisplus.annotation.TableField;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;

/**
 * @author wesley
 */
public class MasterDataTreeEntity extends MasterDataEntity {
    private static final long serialVersionUID = 1L;

    @TableField("parent_code")
    private String parentCode;

    @TableField("parent_id")
    private Long parentId;

    @TableField(exist = false)
    private Boolean leaf;

    @TableField("level")
    private Integer level;

    public String getParentCode() {
        return parentCode;
    }

    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Boolean getLeaf() {
        return leaf;
    }

    public void setLeaf(Boolean leaf) {
        this.leaf = leaf;
    }

}
