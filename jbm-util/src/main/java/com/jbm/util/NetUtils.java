package com.jbm.util;

import lombok.extern.slf4j.Slf4j;

import java.net.*;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class NetUtils {

    /**
     * Given an address resolve it to as many unique addresses or hostnames as can be found.
     *
     * @param address the address to resolve.
     * @return the addresses and hostnames that were resolved from {@code address}.
     */
    public static Set<String> getHostNames(String address) {
        return getHostNames(address, true);
    }

    /**
     * Given an address resolve it to as many unique addresses or hostnames as can be found.
     *
     * @param address         the address to resolve.
     * @param includeLoopback if {@code true} loopback addresses will be included in the returned set.
     * @return the addresses and hostnames that were resolved from {@code address}.
     */
    public static Set<String> getHostNames(String address, boolean includeLoopback) {
        Set<String> hostNames = new HashSet<>(16);

        try {
            InetAddress inetAddress = InetAddress.getByName(address);

            if (inetAddress.isAnyLocalAddress()) {
                try {
                    Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();

                    for (NetworkInterface ni : Collections.list(nis)) {
                        Collections.list(ni.getInetAddresses()).forEach(ia -> {
                            if (ia instanceof Inet4Address) {
                                boolean loopback = ia.isLoopbackAddress();

                                if (!loopback || includeLoopback) {
                                    hostNames.add(ia.getHostName());
                                    hostNames.add(ia.getHostAddress());
                                    hostNames.add(ia.getCanonicalHostName());
                                }
                            }
                        });
                    }
                } catch (SocketException e) {
                    log.warn("Failed to NetworkInterfaces for bind address: {}", address, e);
                }
            } else {
                boolean loopback = inetAddress.isLoopbackAddress();

                if (!loopback || includeLoopback) {
                    hostNames.add(inetAddress.getHostName());
                    hostNames.add(inetAddress.getHostAddress());
                    hostNames.add(inetAddress.getCanonicalHostName());
                }
            }
        } catch (UnknownHostException e) {
            log.warn("Failed to get InetAddress for bind address: {}", address, e);
        }

        return hostNames;
    }
}
