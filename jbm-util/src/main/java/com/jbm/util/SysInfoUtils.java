package com.jbm.util;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.oshi.OshiUtil;
import com.jbm.util.oshi.info.DiskInfo;
import com.jbm.util.oshi.info.MonitorInfo;
import com.jbm.util.oshi.info.NetworkInfo;
import oshi.software.os.OSFileStore;
import oshi.util.FormatUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 获取系统信息工具类
 */
public class SysInfoUtils {

    public static MonitorInfo getMonitorInfoOshi() {
        // 创建一个MonitorInfo对象
        MonitorInfo infoBean = new MonitorInfo();

        // 设置CPU核心数
        infoBean.setCpuCount(OshiUtil.getProcessor().getPhysicalProcessorCount());

        // 设置线程数
        infoBean.setThreadCount(OshiUtil.getProcessor().getLogicalProcessorCount());

        // 设置已用内存
        infoBean.setUsedMemory(FormatUtil.formatBytes(OshiUtil.getMemory().getTotal() - OshiUtil.getMemory().getAvailable()));

        // 设置总内存大小
        infoBean.setTotalMemorySize(FormatUtil.formatBytes(OshiUtil.getMemory().getTotal()));

        // 设置CPU使用率
        infoBean.setCpuRatio(NumberUtil.decimalFormat("#.00", 100d - OshiUtil.getCpuInfo().getFree()) + "%");

        // 设置内存使用率
        infoBean.setMemRatio(NumberUtil.decimalFormat("#.##%", NumberUtil.div(OshiUtil.getMemory().getTotal() - OshiUtil.getMemory().getAvailable(), OshiUtil.getMemory().getTotal())));

        // 返回MonitorInfo对象
        return infoBean;

    }

    /**
     * 获取磁盘信息
     *
     * @return 返回磁盘信息列表
     */
    public static List<DiskInfo> getDiskInfo() {
        List<DiskInfo> list = new ArrayList<>();
        for (OSFileStore fs : OshiUtil.getOs().getFileSystem().getFileStores()) {
            DiskInfo diskInfo = new DiskInfo();

            diskInfo.setPath(fs.getMount());
            diskInfo.setUseSpace(FormatUtil.formatBytes(fs.getTotalSpace() - fs.getUsableSpace()));
            diskInfo.setTotalSpace(FormatUtil.formatBytes(fs.getTotalSpace()));
            if (fs.getTotalSpace() != 0) {
                diskInfo.setPercent(NumberUtil.decimalFormat("#.##%", NumberUtil.div(fs.getTotalSpace() - fs.getUsableSpace(), fs.getTotalSpace())));
            } else {
                diskInfo.setPercent(NumberUtil.decimalFormat("#.##%", 0));
            }

            list.add(diskInfo);
        }
        return list;
    }


    public static List<NetworkInfo> getNetworkInfo() {
        List<NetworkInfo> networkInfos = Collections.synchronizedList(new ArrayList<>());
        OshiUtil.getNetworkIFs().parallelStream().forEach(net -> {
            NetworkInfo networkInfo = new NetworkInfo();
            networkInfo.setName(net.getName());
            networkInfo.setIps(StrUtil.join(",", net.getIPv4addr()));
            if (StrUtil.isEmpty(networkInfo.getIps())) {
                return;
            }
//            String in = FormatUtil.formatBytes(net.getBytesRecv());
//            String out = FormatUtil.formatBytes(net.getBytesSent());
            networkInfo.setSend(NumberUtil.div(net.getBytesSent(), 1024.0));
            networkInfo.setReceive(NumberUtil.div(net.getBytesRecv(), 1024.0));
            networkInfo.setSendStr(FormatUtil.formatBytes(net.getBytesSent()));
            networkInfo.setReceiveStr(FormatUtil.formatBytes(net.getBytesRecv()));
            networkInfo.setTime(DateTime.now());
            networkInfos.add(networkInfo);
        });
        return networkInfos;
    }
}
