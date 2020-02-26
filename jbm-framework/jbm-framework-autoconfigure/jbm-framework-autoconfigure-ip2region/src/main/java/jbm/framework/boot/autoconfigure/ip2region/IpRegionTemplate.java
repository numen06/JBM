package jbm.framework.boot.autoconfigure.ip2region;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.URLUtil;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.DataBlock;
import org.lionsoul.ip2region.DbConfig;
import org.lionsoul.ip2region.DbSearcher;
import org.lionsoul.ip2region.Util;
import org.springframework.beans.factory.InitializingBean;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;


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
    private static final String DB_URL = "https://github.com/lionsoul2014/ip2region/archive/v2.1.0-release.zip";
    private DbConfig config = null;
    private DbSearcher searcher = null;


    /**
     * 初始化IP库
     */
    public void init() {
        try {
            // 因为jar无法读取文件,复制创建临时文件
//            String tmpDir = System.getProperty("user.dir") + File.separator + "temp";
//            String dbPath = tmpDir + File.separator + "ip2region.db";
//            log.info("init ip region db path [{}]", dbPath);
//            File file = new File(dbPath);
            String dbPath = "data/ip2region.db";
            File file = new File(dbPath);
            if (!FileUtil.exist(file)) {

                try {
                    URL url = IpRegionTemplate.class.getClassLoader().getResource("data/ip2region.db");
//                    ClassPathResource resource = new ClassPathResource("result/ip2region.db", IpRegionTemplate.class);
                    log.info("db地址{}", url);
                    if (URLUtil.isFileURL(url)) {
                        dbPath = url.getPath();
                    } else {
                        log.info("路径{}没有发现db文件开始复制", file);
                        FileUtil.writeFromStream(URLUtil.getStream(url), file);
                    }
                } catch (Exception e) {
                    log.error("复制文件发生错误", e);
//                    log.info("开始远程下载{}", DB_URL);
//                    HttpUtil.downloadFile(DB_URL, "ip2region.zip");
//                    ZipUtil.unzip("ip2region.zip");
//                    log.info("下载结束");
                }
            }
            config = new DbConfig();
            searcher = new DbSearcher(config, dbPath);
            log.info("bean [{}]", config);
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
        try {
            //db
            if (searcher == null) {
                log.error("DbSearcher is null");
                return null;
            }
            long startTime = System.currentTimeMillis();
            //查询算法
            int algorithm = DbSearcher.MEMORY_ALGORITYM;
            //B-tree
            //DbSearcher.BINARY_ALGORITHM //Binary
            //DbSearcher.MEMORY_ALGORITYM //Memory
            //define the method
            Method method = null;
            switch (algorithm) {
                case DbSearcher.BTREE_ALGORITHM:
                    method = searcher.getClass().getMethod("btreeSearch", String.class);
                    break;
                case DbSearcher.BINARY_ALGORITHM:
                    method = searcher.getClass().getMethod("binarySearch", String.class);
                    break;
                case DbSearcher.MEMORY_ALGORITYM:
                    method = searcher.getClass().getMethod("memorySearch", String.class);
                    break;
            }

            DataBlock dataBlock = null;
            if (Util.isIpAddress(ip) == false) {
                log.warn("warning: Invalid ip address");
            }
            dataBlock = (DataBlock) method.invoke(searcher, ip);
            String result = dataBlock.getRegion();
            long endTime = System.currentTimeMillis();
            log.debug("region use time[{}] result[{}]", endTime - startTime, result);
            return result;

        } catch (Exception e) {
            log.error("error:{}", e);
        }
        return null;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.init();
    }
}
