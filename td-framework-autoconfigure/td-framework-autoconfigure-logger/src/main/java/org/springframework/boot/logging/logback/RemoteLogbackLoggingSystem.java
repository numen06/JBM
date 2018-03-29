package org.springframework.boot.logging.logback;

import java.security.CodeSource;
import java.security.ProtectionDomain;

import org.slf4j.ILoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.util.Assert;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.jul.LevelChangePropagator;

public class RemoteLogbackLoggingSystem extends LogbackLoggingSystem {

	public RemoteLogbackLoggingSystem(ClassLoader classLoader) {
		super(classLoader);
	}

	@Override
	protected void loadDefaults(LoggingInitializationContext initializationContext, LogFile logFile) {
		LoggerContext context = getLoggerContext();
		stopAndReset(context);
		LogbackConfigurator configurator = new LogbackConfigurator(context);
		context.putProperty("LOG_LEVEL_PATTERN", initializationContext.getEnvironment().resolvePlaceholders("${logging.pattern.level:${LOG_LEVEL_PATTERN:%5p}}"));
		new RemoteDefaultLogbackConfiguration(initializationContext, logFile).apply(configurator);
		context.setPackagingDataEnabled(true);
	}

	@Override
	protected void loadConfiguration(LoggingInitializationContext initializationContext, String location, LogFile logFile) {
		try {
			super.loadConfiguration(initializationContext, location, logFile);
		} catch (IllegalStateException e) {
			this.getLoggerContext().getLogger(RemoteLogbackLoggingSystem.class).error("LogbackLoggingSystem start error", e);
		}
	}

	private void stopAndReset(LoggerContext loggerContext) {
		loggerContext.stop();
		loggerContext.reset();
		if (isBridgeHandlerAvailable()) {
			addLevelChangePropagator(loggerContext);
		}
	}

	private LoggerContext getLoggerContext() {
		ILoggerFactory factory = StaticLoggerBinder.getSingleton().getLoggerFactory();
		Assert.isInstanceOf(LoggerContext.class, factory,
			String.format("LoggerFactory is not a Logback LoggerContext but Logback is on " + "the classpath. Either remove Logback or the competing "
				+ "implementation (%s loaded from %s). If you are using " + "WebLogic you will need to add 'org.slf4j' to " + "prefer-application-packages in WEB-INF/weblogic.xml",
				factory.getClass(), getLocation(factory)));
		return (LoggerContext) factory;
	}

	private Object getLocation(ILoggerFactory factory) {
		try {
			ProtectionDomain protectionDomain = factory.getClass().getProtectionDomain();
			CodeSource codeSource = protectionDomain.getCodeSource();
			if (codeSource != null) {
				return codeSource.getLocation();
			}
		} catch (SecurityException ex) {
			// Unable to determine location
		}
		return "unknown location";
	}

	private void addLevelChangePropagator(LoggerContext loggerContext) {
		LevelChangePropagator levelChangePropagator = new LevelChangePropagator();
		levelChangePropagator.setResetJUL(true);
		levelChangePropagator.setContext(loggerContext);
		loggerContext.addListener(levelChangePropagator);
	}

	@Override
	public void cleanUp() {
		super.cleanUp();
		getLoggerContext().getStatusManager().clear();
	}

	@Override
	protected void reinitialize(LoggingInitializationContext initializationContext) {
		getLoggerContext().reset();
		getLoggerContext().getStatusManager().clear();
		loadConfiguration(initializationContext, getSelfInitializationConfig(), null);
	}

}
