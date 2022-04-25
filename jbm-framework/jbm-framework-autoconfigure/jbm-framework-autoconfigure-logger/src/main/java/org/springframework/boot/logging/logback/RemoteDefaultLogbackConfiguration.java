package org.springframework.boot.logging.logback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySourcesPropertyResolver;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import ch.qos.logback.core.util.OptionHelper;
import jodd.io.FileNameUtil;
import net.logstash.logback.appender.LogstashTcpSocketAppender;
import net.logstash.logback.encoder.LogstashEncoder;
import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;

public class RemoteDefaultLogbackConfiguration extends DefaultLogbackConfiguration {

    private LoggingInitializationContext initializationContext;

    private static final String FILE_LOG_PATTERN = "%d{yyyy-MM-dd HH:mm:ss.SSS} "
            + "${LOG_LEVEL_PATTERN:-%5p} ${PID:- } --- [%t] %-40.40logger{39} : %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}";

    private final PropertyResolver patterns;

    private final LogFile logFile;

    RemoteDefaultLogbackConfiguration(LoggingInitializationContext initializationContext, LogFile logFile) {
        super(logFile);
        this.initializationContext = initializationContext;
        this.patterns = getPatternsResolver(initializationContext.getEnvironment());
        this.logFile = logFile;
    }

    private PropertyResolver getPatternsResolver(Environment environment) {
        PropertySourcesPropertyResolver remoteProperties = new PropertySourcesPropertyResolver(null);
        if (environment == null) {
            return remoteProperties;
        }
        Binder.get(environment).bind("logging.pattern.", Bindable.ofInstance(remoteProperties));
        return remoteProperties;
//		return new PropertySourcesPropertyResolver(environment, "logging.pattern.");
    }

    public void apply(LogbackConfigurator config) {
        synchronized (config.getConfigurationLock()) {
            super.apply(config);
            String url = initializationContext.getEnvironment().resolvePlaceholders("${logging.logstash.url:}");
            if (StringUtils.isNotBlank(url)) {
                Appender<ILoggingEvent> logstashAppender = logstashAppender(config);
                config.root(Level.INFO, logstashAppender);
            }
            if (this.logFile != null) {
                Appender<ILoggingEvent> fileAppender = fileAppender(config, this.logFile.toString());
                config.root(Level.INFO, fileAppender);
            }
        }
    }

    private Appender<ILoggingEvent> logstashAppender(LogbackConfigurator config) {
        LogstashTcpSocketAppender appender = new LogstashTcpSocketAppender();
        String url = initializationContext.getEnvironment().resolvePlaceholders("${logging.logstash.url:}");
        appender.addDestination(url);
        LogstashEncoder encoder = new LogstashEncoder();
        encoder.setEncoding("UTF-8");
        config.start(encoder);
        appender.setEncoder(encoder);
        config.appender("LOGSTASH", appender);
        return appender;
    }

    private Appender<ILoggingEvent> fileAppender(LogbackConfigurator config, String logFile) {
        RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<ILoggingEvent>();
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        String logPattern = this.patterns.getProperty("file", FILE_LOG_PATTERN);
        encoder.setPattern(OptionHelper.substVars(logPattern, config.getContext()));
        appender.setEncoder(encoder);
        config.start(encoder);

        appender.setFile(logFile);

        SizeAndTimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new SizeAndTimeBasedRollingPolicy<ILoggingEvent>();
        String maxHistory = initializationContext.getEnvironment().resolvePlaceholders("${logging.max-history:30}");
        String maxFileSize = initializationContext.getEnvironment()
                .resolvePlaceholders("${logging.max-file-size:50MB}");
        rollingPolicy.setMaxHistory(Integer.parseInt(maxHistory));
        rollingPolicy.setMaxFileSize(FileSize.valueOf(maxFileSize));
        String path = FileNameUtil.getPath(logFile);
        String fileName = FileNameUtil.getBaseName(logFile);
        String ext = FileNameUtil.getExtension(logFile);
        rollingPolicy.setFileNamePattern(path + fileName + ".%d{yyyy-MM-dd}.%i." + ext);
        appender.setRollingPolicy(rollingPolicy);
        rollingPolicy.setParent(appender);
        config.start(rollingPolicy);

        // SizeAndTimeBasedFNATP<ILoggingEvent> triggeringPolicy = new
        // SizeAndTimeBasedFNATP<ILoggingEvent>();
        // triggeringPolicy.setTimeBasedRollingPolicy(rollingPolicy);
        // triggeringPolicy.setMaxFileSize("200Kb");
        // appender.setTriggeringPolicy(triggeringPolicy);
        // config.start(triggeringPolicy);

        config.appender("FILE", appender);
        return appender;
    }
}
