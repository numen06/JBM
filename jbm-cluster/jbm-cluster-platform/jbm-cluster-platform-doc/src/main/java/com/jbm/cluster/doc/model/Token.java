package com.jbm.cluster.doc.model;

import lombok.Data;

@Data
public class Token {

    private Integer expires_in;
    private String token;
    private String wpsUrl;

}
