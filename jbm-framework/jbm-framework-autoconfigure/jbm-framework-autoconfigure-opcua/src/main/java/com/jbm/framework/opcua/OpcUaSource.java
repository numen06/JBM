package com.jbm.framework.opcua;

import cn.hutool.core.util.StrUtil;
import lombok.Data;

@Data
public class OpcUaSource {
    private Boolean enabled  = false;
    private String url;
    private String host;
    private Integer port = 4840;
    private String path;
    private String pointFile;

    public String getUrl() {
        if (StrUtil.isNotBlank(this.url)) {
            return this.url;
        }
        return String.format("opc.tcp://%s:%s%s",
                this.getHost(),
                this.getPort(),
                StrUtil.blankToDefault(this.getPath(), "")
        );
    }
}
