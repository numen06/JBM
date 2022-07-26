package jbm.framework.boot.autoconfigure.ip2region;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.beans.factory.InitializingBean;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * IpRegion服务类
 *
 * @author wesley.zhang
 */
@Slf4j
public class IpRegionTemplate implements InitializingBean {
    /**
     * 远程下载地址
     */
    private static final String DB_URL = "https://gitee.com/lionsoul/ip2region/blob/master/data/ip2region.xdb";
    private static final String DB_PATH = "data/ip2region.xdb";
    private Searcher searcher = null;

    /**
     * 初始化IP库
     */
    public void init() {
        try {
            // 因为jar无法读取文件,复制创建临时文件
//            String tmpDir = System.getProperty("user.dir") + File.separator + "temp";
//            String dbPath = tmpDir + File.separator + "ip2region.db";
//            log.info("init ip region db path [{}]", dbPath);
            File file = new File(DB_PATH);
            if (!FileUtil.exist(file)) {
                try {
                    InputStream inputStream = ResourceUtil.getResource(DB_PATH).openStream();
                    OutputStream outputStream = FileUtil.getOutputStream(file);
                    IoUtil.copy(inputStream, outputStream);
//                    ClassPathResource resource = new ClassPathResource("result/ip2region.db", IpRegionTemplate.class);
                } catch (Exception e) {
                    log.error("复制文件发生错误", e);
//                    log.info("开始远程下载{}", DB_URL);
//                    HttpUtil.downloadFile(DB_URL, file);
//                    log.info("下载结束");
                }
            }
            searcher = Searcher.newWithFileOnly(file.getPath());
            log.info("bean [{}]", searcher);
        } catch (Exception e) {
            log.error("init ip region error:{}", e);
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
            String region = searcher.search(ip);
            return region;
//            System.out.printf("{region: %s, ioCount: %d, took: %d μs}\n", region, searcher.getIOCount(), cost);
        } catch (Exception e) {
            log.error("failed to search(%s): %s\n", ip, e);
            return "未知";
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.init();
    }
}
