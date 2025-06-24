package jbm.framework.boot.autoconfigure.ip2region;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.http.HttpUtil;
import com.google.common.util.concurrent.AbstractScheduledService;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.beans.factory.InitializingBean;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;


/**
 * IpRegion服务类
 *
 * @author wesley.zhang
 */
@Slf4j
public class IpRegionTemplate extends AbstractScheduledService implements InitializingBean {
    /**
     * 远程下载地址
     */
    public static final String DB_URL = "https://gitee.com/lionsoul/ip2region/blob/master/data/ip2region.xdb";
    public static final String DB_PATH = "data/ip2region.xdb";
    public static final String DB_PATH_TEMP = "data/ip2region-temp.xdb";
    private Searcher searcher = null;

    /**
     * 初始化IP库
     */
    public void init() {
        try {
            File tempFile = new File(DB_PATH_TEMP);
            File file = new File(DB_PATH);
            if (FileUtil.exist(tempFile)) {
                // 判断网络是否可以反问，可以访问并且没有文件则下载
                try {
                    InputStream inputStream = FileUtil.getInputStream(tempFile);
                    OutputStream outputStream = FileUtil.getOutputStream(file);
                    IoUtil.copy(inputStream, outputStream);
                } catch (Exception e) {
                    log.error("复制文件发生错误", e);
                }
            } else {
                try {
                    InputStream inputStream = ResourceUtil.getResource(DB_PATH).openStream();
                    OutputStream outputStream = FileUtil.getOutputStream(file);
                    IoUtil.copy(inputStream, outputStream);
                } catch (Exception e) {
                    log.error("复制文件发生错误", e);
                }
            }
            searcher = Searcher.newWithFileOnly(file.getPath());
        } catch (Exception e) {
            log.error("init ip region error", e);
        }
    }


    /**
     * 解析IP
     *
     * @param ip
     * @return
     */
    public String getRegion(String ip) {
        // 2、查询
        try {
            return searcher.search(ip);
        } catch (Exception e) {
            log.error("failed to search({}): {}", ip, e);
            return "未知";
        }
    }

    /**
     * 检查网络是否可达
     */
    private static boolean checkNetwork(String url) {
        try {
            String result = HttpUtil.get(url, 3000); // 超时时间3秒
            return result != null && !result.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public void download(String url, String destPath) {
        boolean isNetworkOk = checkNetwork("https://www.baidu.com");
        if (isNetworkOk) {
            log.info("网络正常，开始下载文件...");
            try {
                File destFile = new File(destPath);
                if (destFile.exists()) {
                    log.info("文件已存在，无需下载。");
                    return;
                }
                HttpUtil.downloadFileFromUrl(url, destFile.getAbsoluteFile());
                log.info("文件下载成功，保存路径：{}", destFile.getAbsoluteFile());
                this.init();
            } catch (Exception e) {
                log.info("文件下载失败", e);
            }
        } else {
            log.info("网络异常，无法下载文件。");
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.init();
        this.startAsync();
    }

    /**
     * @throws Exception
     */
    @Override
    protected void runOneIteration() throws Exception {
        this.download(DB_URL, DB_PATH_TEMP);
    }

    /**
     * @return
     */
    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(0, 1, TimeUnit.DAYS);
    }
}
