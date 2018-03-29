package com.td.framework.devops.actuator.web;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.td.devops.bean.ReleaseOption;
import com.td.framework.devops.actuator.bean.FileInfo;
import com.td.framework.devops.actuator.bean.SourceInfo;
import com.td.framework.devops.actuator.service.SourceService;
import com.td.framework.form.JsonRequestBody;
import com.td.framework.metadata.bean.ResultForm;
import com.td.framework.metadata.exceptions.ServiceException;
import com.td.framework.metadata.usage.page.DataPaging;

@RestController
@RequestMapping("/source")
public class SourceCollection {

	@Autowired
	private SourceService sourceService;

	@RequestMapping("/search")
	public Object search(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		String mianFileName = jsonRequestBody.getString("mainFileName");
		// String extension = jsonRequestBody.getString("extension");
		String dir = jsonRequestBody.getString("dir");
		String extension = StringUtils.isBlank(dir) ? "last" : "info";
		DataPaging<SourceInfo> page = sourceService.searchSourceList(mianFileName, dir, StringUtils.isBlank(extension) ? "last" : extension);
		return ResultForm.createSuccessResultForm(page, "查询列表成功");
	}

	@RequestMapping("/list")
	public Object list(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		return ResultForm.createSuccessResultForm(sourceService.findSourceList(null, "info"), "查询列表成功");
	}

	@RequestMapping("/getSourceDir")
	public Object getSourceDir(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		ResultForm<?> result = null;
		try {
			List<FileInfo> fileList = sourceService.getSourceDir();
			result = ResultForm.createSuccessResultForm(fileList, "查询成功");
		} catch (ServiceException e) {
			result = ResultForm.createErrorResultForm(null, e.getMessage());
			e.printStackTrace();
		}
		return result;
	}

	@RequestMapping("/deploy")
	public Object deploy(HttpServletRequest request) {
		MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
		Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
		Collection<MultipartFile> files = fileMap.values();
		StringBuffer sb = new StringBuffer("upload successful,");
		for (Iterator<MultipartFile> iterator = files.iterator(); iterator.hasNext();) {
			MultipartFile file = (MultipartFile) iterator.next();
			String name = file.getOriginalFilename();
			if (!file.isEmpty()) {
				try {
					String originalFilename = file.getOriginalFilename();
					sourceService.saveSource(originalFilename, file.getBytes());
				} catch (Exception e) {
					return ResultForm.createErrorResultForm(null, "You failed to upload " + name + " => " + e.getMessage());
				}
			} else {
				return ResultForm.createErrorResultForm(null, "You failed to upload " + name + " because the file was empty.");
			}
		}
		return ResultForm.createSuccessResultForm(null, sb.toString());
	}

	@RequestMapping("/releaseStart")
	public Object releaseStart(@RequestBody JsonRequestBody jsonRequestBody) {
		try {
			sourceService.releaseStart(jsonRequestBody.getString("sourceInfoName"));
			return ResultForm.createErrorResultForm(null);
		} catch (Exception e) {
			return ResultForm.createErrorResultForm(null, "ERROR");
		}
	}

	@RequestMapping("/releaseDirStart")
	public Object releaseDirStart(@RequestBody JsonRequestBody jsonRequestBody) {
		ResultForm<?> result = null;
		String sourceInfoName = jsonRequestBody.getString("sourceInfoName");
		String dirname = jsonRequestBody.getString("dirname");
		String path = jsonRequestBody.getString("path");
		boolean open = jsonRequestBody.getBooleanValue("open");
		try {
			String releasePath = sourceService.releaseDesignatedDirStart(path, sourceInfoName, dirname, open);
			result = ResultForm.createSuccessResultForm(releasePath, "发布成功");
		} catch (ServiceException e) {
			result = ResultForm.createErrorResultForm(null, e.getMessage());
		}
		return result;
	}

	@RequestMapping("/releaseDir")
	public Object releaseDir(@RequestBody JsonRequestBody jsonRequestBody) {
		ResultForm<?> result = null;
		String sourceInfoName = jsonRequestBody.getString("sourceInfoName");
		String dirname = jsonRequestBody.getString("dirname");
		String path = jsonRequestBody.getString("path");
		try {
			String releasePath = sourceService.releaseDesignatedDir(path, sourceInfoName, dirname);
			result = ResultForm.createSuccessResultForm(releasePath, "发布成功");
		} catch (ServiceException e) {
			result = ResultForm.createErrorResultForm(null, e.getMessage());
		}
		return result;
	}

	@RequestMapping("/release")
	public Object release(@RequestBody JsonRequestBody jsonRequestBody) {
		try {
			sourceService.release(jsonRequestBody.getString("sourceInfoName"));
			return ResultForm.createErrorResultForm(null);
		} catch (Exception e) {
			return ResultForm.createErrorResultForm(null, "ERROR");
		}
	}

	@RequestMapping("/getRootDir")
	public Object getRootDir(@RequestBody JsonRequestBody jsonRequestBody) {
		ResultForm<?> result = null;
		String sourceInfoName = jsonRequestBody.getString("sourceInfoName");
		String path = jsonRequestBody.getString("path");
		try {
			FileInfo fileInfo = sourceService.getReleaseRootTree(path, sourceInfoName);
			result = ResultForm.createSuccessResultForm(fileInfo, "查询成功");
		} catch (IOException e) {
			e.printStackTrace();
			result = ResultForm.createErrorResultForm(null, e.getMessage());
		} catch (ServiceException e) {
			e.printStackTrace();
			result = ResultForm.createErrorResultForm(null, e.getMessage());
		}
		return result;
	}

	@RequestMapping("/remove")
	public Object remove(@RequestBody JsonRequestBody jsonRequestBody) {
		try {
			String path = jsonRequestBody.getString("path");
			String sourceInfoName = jsonRequestBody.getString("sourceInfoName");
			sourceService.remove(path, sourceInfoName);
			return ResultForm.createErrorResultForm(null);
		} catch (Exception e) {
			return ResultForm.createErrorResultForm(null, "ERROR");
		}
	}

	@RequestMapping("/publish")
	public Object channelRelease(@RequestBody JsonRequestBody jsonRequestBody) {
		ResultForm<?> result = null;
		try {
			ReleaseOption option = jsonRequestBody.tryGet(ReleaseOption.class);
			sourceService.publish(option);
			result = ResultForm.createSuccessResultForm(null, "远程发布成功");
		} catch (IOException e) {
			e.printStackTrace();
			result = ResultForm.createErrorResultForm(null, e.getMessage());
		} catch (ServiceException e) {
			e.printStackTrace();
			result = ResultForm.createErrorResultForm(null, e.getMessage());
		}
		return result;
	}
}
