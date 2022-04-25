package com.jbm.cluster.api.model.entitys.message;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

import java.io.Serializable;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-03-05 03:52
 **/
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public class Notification implements Serializable {
}
