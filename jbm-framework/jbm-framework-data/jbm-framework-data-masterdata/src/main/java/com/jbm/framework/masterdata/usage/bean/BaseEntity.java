package com.jbm.framework.masterdata.usage.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;

/**
 * 技术的实体
 *
 * @author wesley
 */
@MappedSuperclass
@Data
public abstract class BaseEntity implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 7148367690448503947L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;


//	@TableField(value = "delete_flag")
//	@TableLogic
//	private Integer deleteFlag;

    public BaseEntity() {
        super();
    }

    public BaseEntity(Long id) {
        super();
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

//	public Integer getDeleteFlag() {
//		return deleteFlag;
//	}
//
//	public void setDeleteFlag(Integer deleteFlag) {
//		this.deleteFlag = deleteFlag;
//	}


}
