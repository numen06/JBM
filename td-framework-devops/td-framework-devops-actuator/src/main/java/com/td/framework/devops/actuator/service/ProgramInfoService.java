package com.td.framework.devops.actuator.service;

import java.io.File;
import java.util.List;

import com.td.framework.devops.actuator.masterdata.entity.ProgramInfo;
import com.td.framework.metadata.exceptions.ServiceException;
import com.td.framework.service.IAdvSqlService;

public interface ProgramInfoService extends IAdvSqlService<ProgramInfo, Long> {
	List<ProgramInfo> buildProgramInfo(List<File> files) throws ServiceException;

	ProgramInfo buildProgramInfo(File file) throws ServiceException;

	void saveProgrameInfo(ProgramInfo entity) throws ServiceException;

	List<ProgramInfo> findAllProgrameList() throws ServiceException;

	/**
	 * 读取程序信息
	 * 
	 * @param folder
	 * @return
	 * @throws ServiceException
	 */
	ProgramInfo findProgrameInfo(String folder) throws ServiceException;

	void deleteProgrameInfo(String folder) throws ServiceException;

	ProgramInfo toGuard(ProgramInfo entity) throws ServiceException;

	ProgramInfo findProgrameInfoById(Long id) throws ServiceException;

}
