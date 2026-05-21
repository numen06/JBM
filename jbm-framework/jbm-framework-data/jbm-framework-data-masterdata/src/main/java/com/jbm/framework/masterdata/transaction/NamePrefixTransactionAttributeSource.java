package com.jbm.framework.masterdata.transaction;

import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;

import java.lang.reflect.Method;
import java.util.Properties;

/**
 * 按 Service/Business 方法名前缀推断事务属性；规则见 docs/CBSM分层与事务规范.md 第 3.2 节。
 */
public class NamePrefixTransactionAttributeSource implements TransactionAttributeSource {

    private final org.springframework.transaction.annotation.AnnotationTransactionAttributeSource annotationSource =
            new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource();
    private final NameMatchTransactionAttributeSource nameMatchSource;

    public NamePrefixTransactionAttributeSource() {
        this.nameMatchSource = new NameMatchTransactionAttributeSource();
        Properties patterns = new Properties();
        String required = "PROPAGATION_REQUIRED,-java.lang.Exception";
        String readOnly = "PROPAGATION_SUPPORTS,readOnly";
        patterns.setProperty("add*", required);
        patterns.setProperty("update*", required);
        patterns.setProperty("remove*", required);
        patterns.setProperty("clear*", required);
        patterns.setProperty("delete*", required);
        patterns.setProperty("save*", required);
        patterns.setProperty("insert*", required);
        patterns.setProperty("register*", required);
        patterns.setProperty("grant*", required);
        patterns.setProperty("import*", required);
        patterns.setProperty("bind*", required);
        patterns.setProperty("activation*", required);
        patterns.setProperty("activate*", required);
        patterns.setProperty("close*", required);
        patterns.setProperty("reset*", required);
        patterns.setProperty("rest*", required);
        patterns.setProperty("sync*", required);
        patterns.setProperty("enable*", required);
        patterns.setProperty("disable*", required);
        patterns.setProperty("patch*", required);
        patterns.setProperty("merge*", required);
        patterns.setProperty("copy*", required);
        patterns.setProperty("move*", required);
        patterns.setProperty("revoke*", required);
        patterns.setProperty("assign*", required);
        patterns.setProperty("login*", required);
        patterns.setProperty("publish*", required);
        patterns.setProperty("find*", readOnly);
        patterns.setProperty("get*", readOnly);
        patterns.setProperty("select*", readOnly);
        patterns.setProperty("query*", readOnly);
        patterns.setProperty("list*", readOnly);
        patterns.setProperty("page*", readOnly);
        patterns.setProperty("count*", readOnly);
        patterns.setProperty("search*", readOnly);
        patterns.setProperty("retrieval*", readOnly);
        patterns.setProperty("is*", readOnly);
        patterns.setProperty("has*", readOnly);
        patterns.setProperty("load*", readOnly);
        patterns.setProperty("fetch*", readOnly);
        patterns.setProperty("exists*", readOnly);
        patterns.setProperty("check*", readOnly);
        patterns.setProperty("build*", readOnly);
        nameMatchSource.setProperties(patterns);
    }

    @Override
    public TransactionAttribute getTransactionAttribute(Method method, Class<?> targetClass) {
        TransactionAttribute annotationAttr =
                annotationSource.getTransactionAttribute(method, targetClass);
        if (annotationAttr != null) {
            return annotationAttr;
        }
        return nameMatchSource.getTransactionAttribute(method, targetClass);
    }
}