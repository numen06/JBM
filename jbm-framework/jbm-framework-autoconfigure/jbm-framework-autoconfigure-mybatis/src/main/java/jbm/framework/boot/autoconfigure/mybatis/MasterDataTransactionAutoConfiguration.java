package jbm.framework.boot.autoconfigure.mybatis;

import com.jbm.framework.masterdata.transaction.NamePrefixTransactionAttributeSource;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(TransactionInterceptor.class)
@ConditionalOnBean(PlatformTransactionManager.class)
@ConditionalOnProperty(prefix = "jbm.masterdata.transaction", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MasterDataTransactionProperties.class)
@EnableTransactionManagement
@AutoConfigureAfter({TransactionAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class})
public class MasterDataTransactionAutoConfiguration {

    @Bean
    public TransactionAttributeSource masterDataTransactionAttributeSource() {
        return new NamePrefixTransactionAttributeSource();
    }

    @Bean
    public Advisor masterDataServiceTransactionAdvisor(
            PlatformTransactionManager transactionManager,
            TransactionAttributeSource masterDataTransactionAttributeSource,
            MasterDataTransactionProperties properties) {
        return buildAdvisor(transactionManager, masterDataTransactionAttributeSource, properties.getServicePointcut());
    }

    @Bean
    public Advisor masterDataBusinessTransactionAdvisor(
            PlatformTransactionManager transactionManager,
            TransactionAttributeSource masterDataTransactionAttributeSource,
            MasterDataTransactionProperties properties) {
        return buildAdvisor(transactionManager, masterDataTransactionAttributeSource, properties.getBusinessPointcut());
    }

    private static Advisor buildAdvisor(
            PlatformTransactionManager transactionManager,
            TransactionAttributeSource transactionAttributeSource,
            String pointcutExpression) {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression(pointcutExpression);
        TransactionInterceptor interceptor = new TransactionInterceptor(transactionManager, transactionAttributeSource);
        DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor(pointcut, interceptor);
        advisor.setOrder(0);
        return advisor;
    }
}