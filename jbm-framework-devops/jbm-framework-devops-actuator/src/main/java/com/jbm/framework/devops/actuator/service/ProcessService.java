/**
 * 
 */
package com.jbm.framework.devops.actuator.service;

/**
 * @author Leonid
 *
 */
public interface ProcessService {
	
	public boolean exists(Integer pid);
	
//	public Integer exists(Integer pid, String folder);
	
	public boolean kill(Integer pid);
	
	public boolean kill(Integer pid, String folder);
	
	public boolean start(String folder);

	public Integer findPid(String md5);
	
}
