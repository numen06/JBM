package com.jbm.cluster.auth.integration;

/**
 * @author wesley.zhang
 * @date 2018-3-30
 **/
public class IntegrationAuthenticationContext {

    private static ThreadLocal<IntegrationAuthentication> holder = new ThreadLocal<>();

    public static void set(IntegrationAuthentication integrationAuthentication) {
        holder.set(integrationAuthentication);
    }

    public static IntegrationAuthentication get() {
        return holder.get();
    }

    public static void clear() {
        holder.remove();
    }
}
