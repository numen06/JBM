package com.jbm.framework.masterdata.usage.entity;

import lombok.Data;

import javax.persistence.MappedSuperclass;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-02-20 23:55
 **/
@Data
@MappedSuperclass
public class MasterDataCodeEntity extends MasterDataIdEntity {

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
