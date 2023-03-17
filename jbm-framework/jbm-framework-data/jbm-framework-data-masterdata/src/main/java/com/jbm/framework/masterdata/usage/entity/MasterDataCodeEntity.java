package com.jbm.framework.masterdata.usage.entity;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.MappedSuperclass;

/**
 * @program: JBM7
 * @author: wesley.zhang
 * @create: 2020-02-20 23:55
 **/
@Data
@MappedSuperclass
@EqualsAndHashCode(callSuper = true)
public class MasterDataCodeEntity extends MasterDataIdEntity {


    @ApiModelProperty("编码")
    private String code;

    public MasterDataCodeEntity() {
        super();
    }

    public MasterDataCodeEntity(Long id) {
        super(id);
    }

    public MasterDataCodeEntity(Long id, String code) {
        super(id);
        this.code = code;
    }


    public MasterDataCodeEntity(String code) {
        this.code = code;
    }


}
