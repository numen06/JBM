package com.jbm.framework.opcua.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeEventBean {

    private String deviceId;
    private List<Class> eventClassList = new ArrayList<>();
}
