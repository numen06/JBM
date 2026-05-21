package com.jbm.framework.masterdata.business;

import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

/**
 * 业务层基类。子类编排写操作时应通过 {@link #executeInTransaction} 或符合写前缀的 public 方法走 Spring 代理，
 * 同类内复用使用 {@code private doXxx}，禁止 {@code this} 调用另一写入口。
 * <p>
 * 规范见 {@code docs/CBSM分层与事务规范.md}
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
