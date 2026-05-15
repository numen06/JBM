package com.jbm.framework.masterdata.business;

import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

/**
 * 业务层基类（原 PlatformBusinessImpl），子类编排写操作时应通过 {@link #executeInTransaction} 或自身 public 方法上的
 * {@link Transactional} 保证走 Spring 代理，避免同类自调用导致事务失效。
 */
public abstract class BaseBusiness {

    @Transactional(rollbackFor = Exception.class)
    protected <T> T executeInTransaction(Supplier<T> action) {
        return action.get();
    }

    @Transactional(rollbackFor = Exception.class)
    protected void executeInTransaction(Runnable action) {
        action.run();
    }
}