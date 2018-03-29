package com.td.framework.devops.actuator.service;

import java.io.File;
import java.io.IOException;

import org.springframework.stereotype.Service;

import com.td.framework.metadata.exceptions.ServiceException;
import com.td.util.FileUtils;

@Service
public class RecoveryService {

	/**
	 * 彻底回收站文件
	 * 
	 * @param recoveryPath
	 * @throws ServiceException
	 */
	public void remove(String recoveryPath) throws ServiceException {
		try {
			FileUtils.deleteDirectory(new File(recoveryPath));
		} catch (IOException e) {
			throw new ServiceException("删除文件失败");
		}
	}
}
