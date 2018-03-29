/**
 * 
 */
package com.td.framework.devops.actuator.web;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.td.framework.devops.actuator.bean.RecoveryInfo;
import com.td.framework.devops.actuator.service.RecoveryService;
import com.td.framework.devops.actuator.service.SourceService;
import com.td.framework.devops.actuator.service.impl.ProgramServiceImpl;
import com.td.framework.form.JsonRequestBody;
import com.td.framework.metadata.bean.ResultForm;
import com.td.framework.metadata.exceptions.ServiceException;
import com.td.framework.metadata.usage.page.DataPaging;

/**
 * @author Leonid
 *
 */
@RestController
@RequestMapping("/recovery")
public class RecoveryController {

	private static final Logger logger = LoggerFactory.getLogger(RecoveryController.class);

	@Autowired
	private ProgramServiceImpl programServiceImpl;
	@Autowired
	private SourceService sourceService;
	@Autowired
	private RecoveryService recoveryService;

	@RequestMapping("/search")
	public Object search(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		ResultForm<?> result = null;
		String filename = jsonRequestBody.getString("filename");
		String type = jsonRequestBody.getString("type");
		try {
			DataPaging<RecoveryInfo> page = programServiceImpl.findRecoveryProgramList(filename, type);
			result = ResultForm.createSuccessResultForm(page, "查询成功");
		} catch (IOException e) {
			logger.error(e.getMessage());
			result = ResultForm.createErrorResultForm(null, e.getMessage());
			e.printStackTrace();
		}
		return result;
	}

	@RequestMapping("/reduction")
	public Object reduction(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		ResultForm<?> result = null;
		String recoveryPath = jsonRequestBody.getString("recoveryPath");
		String sourcePath = jsonRequestBody.getString("sourcePath");
		boolean replace = jsonRequestBody.getBooleanValue("replace");
		String type = jsonRequestBody.getString("type");
		try {
			if (type.equals("source")) {
				sourceService.reduction(recoveryPath, sourcePath, replace);
			} else {
				programServiceImpl.reduction(recoveryPath, sourcePath, replace);
			}
			result = ResultForm.createSuccessResultForm(null, "还原成功");
		} catch (IOException e) {
			logger.error(e.getMessage());
			result = ResultForm.createErrorResultForm(null, e.getMessage());
			e.printStackTrace();
		} catch (ServiceException e) {
			logger.error(e.getMessage());
			result = ResultForm.createErrorResultForm(null, e.getMessage());
			e.printStackTrace();
		}
		return result;
	}

	@RequestMapping("/remove")
	public Object remove(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		ResultForm<?> result = null;
		String recoveryPath = jsonRequestBody.getString("recoveryPath");
		// String sourcePath = jsonRequestBody.getString("sourcePath");
		// boolean replace = jsonRequestBody.getBooleanValue("replace");
		// String type = jsonRequestBody.getString("type");
		try {
			recoveryService.remove(recoveryPath);
			result = ResultForm.createSuccessResultForm(null, "彻底删除成功");
		} catch (ServiceException e) {
			logger.error(e.getMessage());
			result = ResultForm.createErrorResultForm(null, e.getMessage());
			e.printStackTrace();
		}
		return result;
	}
}
