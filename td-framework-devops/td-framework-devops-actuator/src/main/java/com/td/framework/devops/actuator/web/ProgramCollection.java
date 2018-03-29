package com.td.framework.devops.actuator.web;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.td.framework.devops.actuator.masterdata.entity.ProgramInfo;
import com.td.framework.devops.actuator.service.ProgramInfoService;
import com.td.framework.devops.actuator.service.impl.ProgramServiceImpl;
import com.td.framework.form.JsonRequestBody;
import com.td.framework.metadata.bean.ResultForm;
import com.td.framework.metadata.exceptions.ServiceException;

import freemarker.template.utility.NullArgumentException;

@RestController
@RequestMapping("/program")
public class ProgramCollection {

	@Autowired
	private ProgramServiceImpl programServiceImpl;

	@Autowired
	private ProgramInfoService programInfoService;

	@RequestMapping("/recovery")
	public Object recovery(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		ResultForm<?> result = null;
		String filepath = jsonRequestBody.getString("filepath");
		try {
			programServiceImpl.recoveryProcess(filepath);
			result = ResultForm.createSuccessResultForm(null, "已成功移除到回收站");
		} catch (ServiceException e) {
			result = ResultForm.createErrorResultForm(null, e.getMessage());
			// e.printStackTrace();
		}
		return result;
	}

	@RequestMapping("/search")
	public Object search(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		ResultForm<?> result = null;
		String mainFileName = jsonRequestBody.getString("mainFileName");
		Boolean status = jsonRequestBody.getBoolean("status");
		try {
			result = ResultForm.createSuccessResultForm(programServiceImpl.searchProgramList(mainFileName, status), "查询列表成功");
		} catch (ServiceException e) {
			return ResultForm.createErrorResultForm(null, "查询列表失败");
		}
		return result;
	}

	@RequestMapping("/list")
	public Object list(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		return ResultForm.createSuccessResultForm(programServiceImpl.scanningProgramList(), "查询列表成功");
	}

	@RequestMapping("/kill")
	public Object killProgram(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		try {
			ProgramInfo info = jsonRequestBody.tryGet(ProgramInfo.class);
			if (info.getFolder() == null)
				throw new NullArgumentException();
			programServiceImpl.killProgram(info.getFolder());
			return ResultForm.createSuccessResultForm(null, "结束进程成功");
		} catch (ServiceException e) {
			return ResultForm.createErrorResultForm(null, e.getMessage());
		} catch (Exception e) {
			return ResultForm.createErrorResultForm(null, "结束进程失败");
		}
	}

	@RequestMapping("/start")
	public Object startProgram(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		try {
			ProgramInfo info = jsonRequestBody.tryGet(ProgramInfo.class);
			if (info.getFolder() == null)
				throw new NullArgumentException();
			programServiceImpl.startProgram(info.getFolder());
			return ResultForm.createSuccessResultForm(null, "开启进程成功");
		} catch (ServiceException e) {
			return ResultForm.createErrorResultForm(null, e.getMessage());
		} catch (Exception e) {
			return ResultForm.createErrorResultForm(null, "开启进程失败");
		}
	}

	@RequestMapping("/toGuard")
	public Object toGuard(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		try {
			ProgramInfo info = jsonRequestBody.tryGet(ProgramInfo.class);
			if (info.getFolder() == null)
				throw new NullArgumentException();
			programInfoService.toGuard(info);
			return ResultForm.createSuccessResultForm(null, info.getGuard() ? "守护开启成功" : "守护关闭成功");
		} catch (ServiceException e) {
			return ResultForm.createErrorResultForm(null, e.getMessage());
		} catch (Exception e) {
			return ResultForm.createErrorResultForm(null, "守护操作失败");
		}
	}

	@RequestMapping("/getGcUtilMap")
	public Object getGcUtilMap(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		ResultForm<?> result = null;
		String folder = jsonRequestBody.getString("folder");
		try {
			Map<String, String> resMap = programServiceImpl.getGcInfoMap(folder);
			if (resMap == null) {
				result = ResultForm.createErrorResultForm(null, "当前进程未开启");
			} else {
				result = ResultForm.createSuccessResultForm(resMap, "查询成功");
			}
		} catch (NumberFormatException e) {
			result = ResultForm.createErrorResultForm(null, e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			result = ResultForm.createErrorResultForm(null, e.getMessage());
			e.printStackTrace();
		} catch (ServiceException e) {
			result = ResultForm.createErrorResultForm(null, e.getMessage());
			e.printStackTrace();
		}
		return result;
	}

	@RequestMapping("/refresh")
	public Object refreshProgram(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		try {
			ProgramInfo info = jsonRequestBody.tryGet(ProgramInfo.class);
			Boolean cache = jsonRequestBody.getBoolean("cache");
			if (cache == null) {
				cache = true;
			}
			if (info.getFolder() == null)
				throw new NullArgumentException();
			info = programServiceImpl.refreshProgram(info);
			return ResultForm.createSuccessResultForm(info, "刷新进程信息成功");
		} catch (Exception e) {
			return ResultForm.createErrorResultForm(null, "刷新进程信息失败");
		}
	}

	@RequestMapping("/structure")
	public Object structureProgram(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		try {
			ProgramInfo info = jsonRequestBody.tryGet(ProgramInfo.class);
			if (info.getMianFile() == null)
				throw new NullArgumentException();
			if (info.getFolder() == null)
				throw new NullArgumentException();
			programServiceImpl.structure(info);
			return ResultForm.createSuccessResultForm(null, "初始化进程成功");
		} catch (Exception e) {
			return ResultForm.createErrorResultForm(null, "初始化进程失败");
		}
	}

	@RequestMapping("/scanning")
	public Object scanningProgramList(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		try {
			// ProgramInfo info = jsonRequestBody.tryGet(ProgramInfo.class);
			programServiceImpl.scanningProgramList();
			return ResultForm.createSuccessResultForm(null, "扫描进程成功");
		} catch (Exception e) {
			return ResultForm.createErrorResultForm(null, "扫描进程失败");
		}
	}
}
