package com.jbm.framework.devops.actuator.service.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.jbm.framework.devops.actuator.mapper.ProgramInfoMapper;
import com.jbm.framework.devops.actuator.masterdata.entity.ProgramInfo;
import com.jbm.framework.devops.actuator.service.ProgramInfoService;
import com.jbm.framework.metadata.exceptions.ServiceException;
import com.jbm.framework.service.mybatis.AdvSqlDaoImpl;
import com.jbm.util.StringUtils;

import jodd.io.FileNameUtil;

@Service
public class ProgramInfoServiceImpl extends AdvSqlDaoImpl<ProgramInfoMapper, ProgramInfo, String> implements ProgramInfoService {

	private final static Logger logger = LoggerFactory.getLogger(ProgramServiceImpl.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.jbm.framework.devops.actuator.service.impl.safs#buildProgramInfo(java.
	 * util.List)
	 */
	@Override
	public List<ProgramInfo> buildProgramInfo(List<File> files) throws ServiceException {
		List<ProgramInfo> list = new ArrayList<ProgramInfo>();
		for (Iterator<File> iterator = files.iterator(); iterator.hasNext();) {
			File file = iterator.next();
			ProgramInfo info = buildProgramInfo(file);
			list.add(info);
		}
		return list;
	}

	/**
	 * @param file
	 *            jar文件的地址
	 * @return
	 * @throws ServiceException
	 */
	@Override
	public ProgramInfo buildProgramInfo(File file) throws ServiceException {
		ProgramInfo info = null;
		final String folder = FileNameUtil.normalize(file.getParentFile().getPath(), true);
		try {
			info = findProgrameInfo(folder);
		} catch (ServiceException e) {
			logger.info("查找程序信息错误");
		}
		if (info == null) {
			info = new ProgramInfo();
			info.setStauts(false);
		}
		info.setFolder(folder);
		if (info.getCreateTime() == null)
			info.setCreateTime(new Date());
		info.setMianFile(file.getName());
		this.saveProgrameInfo(info);
		return info;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.jbm.framework.devops.actuator.service.impl.safs#saveProgrameInfo(com.
	 * jbm.framework.devops.actuator.masterdata.entity.ProgramInfo)
	 */
	@Override
	public void saveProgrameInfo(ProgramInfo entity) throws ServiceException {
		if (StringUtils.isBlank(entity.getFolder()))
			throw new ServiceException("主程序位置存在");
		try {
			this.save(entity);
			// this.levelTemplate.put(TABLE, info.getId(), info);
		} catch (Exception e) {
			throw new ServiceException("保存程序信息错误", e);
		}
	}

	@Override
	public void deleteProgrameInfo(String folder) throws ServiceException {
		try {
			ProgramInfo info = this.findProgrameInfo(folder);
			this.delete(info);
		} catch (Exception e) {
			throw new ServiceException("删除程序信息错误", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.jbm.framework.devops.actuator.service.impl.safs#findAllProgrameList()
	 */
	@Override
	public List<ProgramInfo> findAllProgrameList() throws ServiceException {
		return this.selectAllEntitys();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.jbm.framework.devops.actuator.service.impl.safs#findProgrameInfo(java.
	 * lang.String)
	 */
	@Override
	public ProgramInfo findProgrameInfo(String folder) throws ServiceException {
		ProgramInfo info = new ProgramInfo();
		info.setFolder(folder);
		info.setGuard(null);
		// info.setId(folder);
		// try {
		// String text = FileUtils.readFileToString(new
		// File(FileNameUtil.concat(folder, "programe.info", true)));
		// info = JSON.parseObject(text, ProgramInfo.class);
		// } catch (IOException e) {
		// } finally {
		// info.setFolder(folder);
		// }
		// return info;
		if (StringUtils.isBlank(info.getFolder()))
			throw new ServiceException("主程序位置存在");
		try {
			// info = (ProgramInfo) this.levelTemplate.get(TABLE, info.getId());
			// this.findProgrameInfo(folder);
			info = this.selectEntity(info);
		} catch (Exception e) {
			throw new ServiceException("获取程序信息错误", e);
		}
		return info;
	}

	@Override
	public ProgramInfo toGuard(ProgramInfo entity) throws ServiceException {
		ProgramInfo dbInfo = this.findProgrameInfo(entity.getFolder());
		dbInfo.setGuard(entity.getGuard());
		this.saveProgrameInfo(dbInfo);
		return dbInfo;
	}

	@Override
	public ProgramInfo findProgrameInfoById(Long id) throws ServiceException {
		ProgramInfo dbInfo = this.selectByPrimaryKey(id);
		return dbInfo;
	}

}
