package com.jbm.cluster.platform.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.core.DelegatingServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Keeps dynamic lb:// routing while avoiding stale Nacos instances during local jaja7 runs.
 */
@Configuration(proxyBeanMethods = false)
@LoadBalancerClients(defaultConfiguration = PreferLocalLoadBalancerConfiguration.PreferLocalInstanceSupplierConfiguration.class)
public class PreferLocalLoadBalancerConfiguration {

    @Slf4j
    static class PreferLocalInstanceSupplierConfiguration {

        @Bean
        @ConditionalOnProperty(prefix = "jbm.gateway.loadbalancer", name = "prefer-local", havingValue = "true")
        ServiceInstanceListSupplier preferLocalServiceInstanceListSupplier(ConfigurableApplicationContext context) {
            ServiceInstanceListSupplier delegate = ServiceInstanceListSupplier.builder()
                    .withDiscoveryClient()
                    .withCaching()
                    .build(context);
            return new PreferLocalServiceInstanceListSupplier(delegate, localAddresses());
        }

        private static Set<String> localAddresses() {
            Set<String> addresses = new LinkedHashSet<>();
            addresses.add("127.0.0.1");
            addresses.add("localhost");
            try {
                addresses.add(InetAddress.getLocalHost().getHostAddress());
                addresses.add(InetAddress.getLocalHost().getHostName());
            } catch (Exception ignored) {
                // NetworkInterface scan below is the primary source on multi-NIC machines.
            }
            try {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface ni = interfaces.nextElement();
                    Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
                    while (inetAddresses.hasMoreElements()) {
                        InetAddress address = inetAddresses.nextElement();
                        addresses.add(address.getHostAddress());
                        addresses.add(address.getHostName());
                    }
                }
            } catch (SocketException ignored) {
                // Keep the loopback/local-host fallbacks.
            }
            return Collections.unmodifiableSet(addresses);
        }
    }

    @Slf4j
    static class PreferLocalServiceInstanceListSupplier extends DelegatingServiceInstanceListSupplier {

        private final Set<String> localAddresses;

        PreferLocalServiceInstanceListSupplier(ServiceInstanceListSupplier delegate, Set<String> localAddresses) {
            super(delegate);
            this.localAddresses = localAddresses;
            log.info("Prefer-local load balancer enabled for [{}], localAddresses={}", delegate.getServiceId(), localAddresses);
        }

        @Override
        public Flux<List<ServiceInstance>> get() {
            return delegate.get().map(this::preferLocalInstances);
        }

        private List<ServiceInstance> preferLocalInstances(List<ServiceInstance> instances) {
            if (instances == null || instances.size() <= 1) {
                return instances;
            }
            List<ServiceInstance> localInstances = instances.stream()
                    .filter(instance -> localAddresses.contains(instance.getHost()))
                    .collect(Collectors.toList());
            if (localInstances.isEmpty()) {
                return instances;
            }
            if (localInstances.size() != instances.size()) {
                log.debug("Prefer local instances for service [{}]: {} of {}", getServiceId(), localInstances.size(), instances.size());
            }
            return localInstances;
        }
    }
}
