/**
 * 
 */
package com.td.framework.devops.actuator.monitor;

import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.pfmiles.org.apache.commons.lang.StringUtils;
import com.td.framework.devops.actuator.masterdata.entity.ProgramInfo;
import com.td.framework.devops.actuator.service.ProgramInfoService;
import com.td.framework.devops.actuator.service.impl.ProgramServiceImpl;
import com.td.framework.metadata.exceptions.ServiceException;

/**
 * @author Leonid
 *
 */
@Service
public class TurnOnInit {
	private final static Logger logger = LoggerFactory.getLogger(TurnOnInit.class);

	@Autowired
	private ProgramServiceImpl programService;

	@Autowired
	private ProgramInfoService programInfoService;

	/**
	 * 
	 */
	public TurnOnInit() {
		// TODO Auto-generated constructor stub
	}

	@PostConstruct
	private void init() {
		// DataPaging<ProgramInfo> page = programService.findProgramList();
		try {
			List<ProgramInfo> infos = programInfoService.findAllProgrameList();
			for (ProgramInfo info : infos) {
				if (StringUtils.isBlank(info.getFolder()))
					continue;
				try {
					if (info.getStauts())
						programService.startProgram(info.getFolder());
				} catch (ServiceException e) {
					logger.error("启动程序[{}]失败", info.getFolder(), e);
				}
				// programService.startGuard(info);
			}
		} catch (Exception e) {
			logger.error("初始化启动程序失败", e);
		}
	}

}
