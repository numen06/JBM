package com.jbm.framework.masterdata.usage.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.util.Date;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-02-20 23:55
 **/
@Data
@MappedSuperclass
public class MasterDataIdEntity extends MasterDataEntity {


    public MasterDataIdEntity() {
        super();
    }

    public MasterDataIdEntity(Long id) {
        super();
        this.id = id;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = com.baomidou.mybatisplus.annotation.IdType.ID_WORKER)
    private Long id;


}
