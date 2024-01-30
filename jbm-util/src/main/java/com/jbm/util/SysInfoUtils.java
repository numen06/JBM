package com.jbm.util;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.oshi.OshiUtil;
import com.jbm.util.oshi.info.DiskInfo;
import com.jbm.util.oshi.info.MonitorInfo;
import com.jbm.util.oshi.info.NetworkInfo;
import lombok.extern.slf4j.Slf4j;
import oshi.software.os.OSFileStore;
import oshi.util.FormatUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 获取系统信息工具类
 */
@Slf4j
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


    /**
     * <p>
     * 是否忽略此网卡
     * </p>
     *
     * @param iPv4addr 网卡地址
     * @param macAddr  MAC地址
     * @param netName  网卡名字
     * @return 是/否
     * @author 皮锋
     * @custom.date 2022/5/31 10:13
     */
    private static boolean ignore(String[] iPv4addr, String macAddr, String netName) {
        return ArrayUtil.isEmpty(iPv4addr)
                // 127.0.0.1
                || ArrayUtil.contains(iPv4addr, "127.0.0.1")
                // MAC地址不存在
                || "00:00:00:00:00:00".equals(macAddr)
                // 0.0.0.0
                || ArrayUtil.contains(iPv4addr, "0.0.0.0")
                // docker
                || StrUtil.containsIgnoreCase(netName, "docker")
                // lo
                || StrUtil.containsIgnoreCase(netName, "lo");
    }

    public static List<NetworkInfo> getNetworkInfo() {
        List<NetworkInfo> networkInfos = Collections.synchronizedList(new ArrayList<>());
        OshiUtil.getNetworkIFs().parallelStream().forEach(net -> {
            NetworkInfo networkInfo = new NetworkInfo();
            // 网卡地址
            String[] iPv4Addrs = net.getIPv4addr();
            // 掩码长度
//				Short[] subnetMasks = net.getSubnetMasks();
            // MAC地址
            String macAddr = net.getMacaddr().toUpperCase();
            // 网卡名字
            String netName = net.getName();
            // 网卡描述信息
//				String displayName = net.getDisplayName();
            // 是否忽略此网卡
            if (ignore(iPv4Addrs, macAddr, netName)) {
                return;
            }
            networkInfo.setName(net.getName());
            networkInfo.setIpV4Addr(StrUtil.join(",", iPv4Addrs));
            networkInfo.setMacAddr(macAddr);
            // 网速
            long start = System.currentTimeMillis();
            long rxBytesStart = net.getBytesRecv();
            long txBytesStart = net.getBytesSent();
            // 休眠一秒
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("线程中断异常！", e);
            }
            long end = System.currentTimeMillis();
            // 更新此接口上的接口网络统计信息
            net.updateAttributes();
            long rxBytesEnd = net.getBytesRecv();
            long txBytesEnd = net.getBytesSent();
            // 网卡配置
            networkInfo.setType("Ethernet");
//				netInterfaceDomain.setMask(Ipv4Util.getMaskByMaskBit(subnetMasks[0]));
//				netInterfaceDomain.setBroadcast(Ipv4Util.getEndIpStr(iPv4addr[0], (int) subnetMasks[0]));
//				netInterfaceDomain.setHwAddr(macAddr);
//				netInterfaceDomain.setDescription(displayName);
//				// 网卡状态
//				netInterfaceDomain.setRxBytes(net.getBytesRecv());
//				netInterfaceDomain.setRxDropped(net.getInDrops());
//				netInterfaceDomain.setRxErrors(net.getInErrors());
//				netInterfaceDomain.setRxPackets(net.getPacketsRecv());
//				netInterfaceDomain.setTxBytes(net.getBytesSent());
//				netInterfaceDomain.setTxDropped(net.getCollisions());
//				netInterfaceDomain.setTxErrors(net.getOutErrors());
//				netInterfaceDomain.setTxPackets(net.getPacketsSent());

            // 1Byte=8bit
            double rxBps = (double) (rxBytesEnd - rxBytesStart) / ((double) (end - start) / 1000);
            double txBps = (double) (txBytesEnd - txBytesStart) / ((double) (end - start) / 1000);
            // 网速
            networkInfo.setDownload(rxBps);
            networkInfo.setUpload(txBps);
            //添加到网卡列表
            networkInfos.add(networkInfo);
        });
        return networkInfos;
    }
}
