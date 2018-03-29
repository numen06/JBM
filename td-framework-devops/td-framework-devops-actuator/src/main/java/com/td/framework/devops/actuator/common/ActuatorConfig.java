package com.td.framework.devops.actuator.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "actuator")
public class ActuatorConfig {

//	private Boolean guard = false;

	private final static String SOURCE_ROOT = "target/sources/";

	private final static String RELEASE_ROOT = "target/release/";

	private final static String RECOVERY_ROOT = "target/recovery/";

	private final static String SOURCE_RECOVERY_ROOT = "source/";

	private final static String PROCESS_RECOVERY_ROOT = "process/";

	private final static String WINDOWS = "Win";

	private static String SYSTEM_TYPE = System.getProperty("os.name");

	private String releasePath = RELEASE_ROOT;
	private String sourcePath = SOURCE_ROOT;
	private String recoveryPath = RECOVERY_ROOT;
	private String sourceRecoveryPath = RECOVERY_ROOT + SOURCE_RECOVERY_ROOT;
	private String processRecoveryPath = RECOVERY_ROOT + PROCESS_RECOVERY_ROOT;

	private String searchPath = releasePath;

//	public Boolean isGuard() {
//		return guard;
//	}

	public String getSourceRecoveryPath() {
		return sourceRecoveryPath;
	}

	public void setSourceRecoveryPath(String sourceRecoveryPath) {
		this.sourceRecoveryPath = sourceRecoveryPath;
	}

	public String getProcessRecoveryPath() {
		return processRecoveryPath;
	}

	public void setProcessRecoveryPath(String processRecoveryPath) {
		this.processRecoveryPath = processRecoveryPath;
	}

	public String getRecoveryPath() {
		return recoveryPath;
	}

	public void setRecoveryPath(String recoveryPath) {
		this.recoveryPath = recoveryPath;
	}

	public static boolean isWindows() {
		return SYSTEM_TYPE.indexOf(WINDOWS) != -1;
	}

	public String getReleasePath() {
		return releasePath;
	}

	public void setReleasePath(String releasePath) {
		this.releasePath = releasePath;
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	public String getSearchPath() {
		return searchPath;
	}

	public void setSearchPath(String searchPath) {
		this.searchPath = searchPath;
	}

//	public Boolean getGuard() {
//		return guard;
//	}
//
//	public void setGuard(Boolean guard) {
//		this.guard = guard;
//	}

}
