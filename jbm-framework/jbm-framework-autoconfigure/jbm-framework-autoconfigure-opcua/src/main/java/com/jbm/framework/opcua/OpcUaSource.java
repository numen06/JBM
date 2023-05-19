package com.jbm.framework.opcua;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;

@Data
public class OpcUaSource {
    private Boolean carryQuote = true;
    private Boolean enabled = false;
    private String url;
    private String host;
    private Integer port = 4840;
    private String path;
    private String pointFile;

    public String getUrl() {
        if (StrUtil.isNotBlank(this.url)) {
            this.host = StrUtil.subBetween(url, "//", ":");
            this.port = NumberUtil.parseInt(StrUtil.subAfter(url, ":", true));
            return this.url;
        }
        return String.format("opc.tcp://%s:%s%s",
                this.getHost(),
                this.getPort(),
                StrUtil.blankToDefault(this.getPath(), "")
        );
    }
}
