package jbm.framework.boot.autoconfigure.fastdfs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fastdfs")
public class FastdfsProperties {

    private String trackerServers;

    public String getTrackerServers() {
        return trackerServers;
    }

    public void setTrackerServers(String trackerServers) {
        this.trackerServers = trackerServers;
    }

}