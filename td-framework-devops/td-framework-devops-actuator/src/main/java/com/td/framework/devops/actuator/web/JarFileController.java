/**
 * 
 */
package com.td.framework.devops.actuator.web;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.google.common.base.Charsets;
import com.td.framework.devops.actuator.bean.FileInfo;
import com.td.framework.devops.actuator.service.SourceService;
import com.td.framework.devops.actuator.service.impl.FolderFileServiceImpl;
import com.td.framework.form.JsonRequestBody;
import com.td.framework.metadata.bean.ResultForm;
import com.td.framework.metadata.exceptions.ServiceException;

import jodd.io.FileNameUtil;
import jodd.util.MimeTypes;

/**
 * @author Leonid
 *
 */
@RestController
@RequestMapping("/file")
public class JarFileController {

	private static Logger logger = LoggerFactory.getLogger(JarFileController.class);

	@Autowired
	private FolderFileServiceImpl folderFileServiceImpl;
	@Autowired
	private SourceService sourceService;

	@RequestMapping("/isRepeatFile")
	public Object isRepeatFile(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		ResultForm<?> result = null;
		String fileMd5 = jsonRequestBody.getString("fileMd5");
		String filename = jsonRequestBody.getString("filename");
		try {
			boolean res = sourceService.isRepeatFile(fileMd5, filename);
			result = ResultForm.createSuccessResultForm(res, "查询成功");
		} catch (IOException e) {
			result = ResultForm.createErrorResultForm(null, e.getMessage());
			e.printStackTrace();
		} catch (ServiceException e) {
			result = ResultForm.createErrorResultForm(null, e.getMessage());
			e.printStackTrace();
		}
		return result;
	}

	@RequestMapping("/writeFile")
	public Object writeFile(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		ResultForm<?> result = null;
		String text = jsonRequestBody.getString("text");
		String filepath = jsonRequestBody.getString("filepath");
		try {
			folderFileServiceImpl.writeStringtoFile(filepath, text);
			result = ResultForm.createSuccessResultForm(null, "保存成功");
		} catch (IOException e) {
			e.printStackTrace();
			result = ResultForm.createErrorResultForm(null, e.getMessage());
		}
		return result;
	}

	@RequestMapping("/readFile")
	public Object readFile(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		ResultForm<?> result = null;
		String filepath = jsonRequestBody.getString("filepath");
		try {
			String info = folderFileServiceImpl.readFiletoString(filepath);
			result = ResultForm.createSuccessResultForm(info, "获取成功");
		} catch (IOException e) {
			result = ResultForm.createErrorResultForm(null, e.getMessage());
			e.printStackTrace();
		}
		return result;
	}

	@RequestMapping("/renameDir")
	public Object renameDir(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		ResultForm<?> result = null;
		String dirpath = jsonRequestBody.getString("dirpath");
		String dirname = jsonRequestBody.getString("dirname");
		try {
			String newPath = folderFileServiceImpl.renameFile(dirpath, dirname);
			result = ResultForm.createSuccessResultForm(newPath, "修改成功");
		} catch (IOException e) {
			result = ResultForm.createErrorResultForm(null, e.getMessage());
			e.printStackTrace();
		} catch (ServiceException e) {
			result = ResultForm.createErrorResultForm(null, e.getMessage());
			e.printStackTrace();
		}
		return result;
	}

	@RequestMapping("/makeDir")
	public Object makeDir(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		ResultForm<?> result = null;
		String dirpath = jsonRequestBody.getString("dirpath");
		String dirname = jsonRequestBody.getString("dirname");
		try {
			boolean res = folderFileServiceImpl.makeDir(dirpath, dirname);
			if (res) {
				result = ResultForm.createSuccessResultForm(null, "创建成功");
			} else {
				result = ResultForm.createErrorResultForm(null, "创建失败");
			}
		} catch (IOException e) {
			result = ResultForm.createErrorResultForm(null, e.getMessage());
			e.printStackTrace();
		}
		return result;
	}

	@RequestMapping("/upload")
	public Object upload(HttpServletRequest request) {
		String uploadPath = request.getParameter("uploadPath");
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
					folderFileServiceImpl.uploadFile(uploadPath, originalFilename, file.getBytes());
				} catch (Exception e) {
					return ResultForm.createErrorResultForm(null, "You failed to upload " + name + " => " + e.getMessage());
				}
			} else {
				return ResultForm.createErrorResultForm(null, "You failed to upload " + name + " because the file was empty.");
			}
		}
		return ResultForm.createSuccessResultForm(null, sb.toString());
	}

	@RequestMapping("/deleteFile")
	public Object deleteFile(String path) {
		ResultForm<?> result = null;
		try {
			folderFileServiceImpl.deleteFile(path);
			result = ResultForm.createSuccessResultForm(null, "删除成功");
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			result = ResultForm.createErrorResultForm(null, e.getMessage());
		}
		return result;
	}

	@RequestMapping("/downloadFile")
	public void downloadFile(HttpServletRequest request, HttpServletResponse response, String path) {
		try {
			byte[] byteArr = folderFileServiceImpl.downloadFile(path);
			String filename = FileNameUtil.getName(path);
			response.setCharacterEncoding("utf-8");
			response.setContentType(MimeTypes.getMimeType(FileNameUtil.getExtension(filename)));
			response.setHeader("Content-Disposition", "attachment;fileName=" + URLEncoder.encode(filename, Charsets.UTF_8.name()));
			ServletOutputStream os = response.getOutputStream();
			os.write(byteArr);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@RequestMapping("/previewFile")
	public void previewFile(HttpServletRequest request, HttpServletResponse response, String path) {
		// String path = FileNameUtil.normalize(request.getParameter("path"),
		// true);
		try {
			byte[] byteArr = folderFileServiceImpl.downloadFile(path);
			String filename = FileNameUtil.getName(path);
			response.setCharacterEncoding("utf-8");
			ServletOutputStream os = response.getOutputStream();
			os.write(byteArr);
			String ext = FileNameUtil.getExtension(filename);
			// if ("log".equalsIgnoreCase(ext))
			// return;
			response.setContentType(MimeTypes.getMimeType(ext));
			response.setHeader("Content-Disposition", "inline;fileName=" + URLEncoder.encode(filename, Charsets.UTF_8.name()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@RequestMapping("/getTreeFile")
	public Object getTreeFile(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		String folderPath = jsonRequestBody.getString("folderPath");
		ResultForm<?> result = null;
		try {
			FileInfo fileInfo = folderFileServiceImpl.getTreeFile(folderPath);
			fileInfo.setRootFolder(true);
			result = ResultForm.createSuccessResultForm(fileInfo, "查询成功");
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			result = ResultForm.createErrorResultForm(null, e.getMessage());
		}
		return result;

	}

	@RequestMapping("/getRootFile")
	public Object getRootFile(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		String folderPath = jsonRequestBody.getString("folderPath");
		ResultForm<?> result = null;
		try {
			FileInfo fileInfo = folderFileServiceImpl.getFileByPath(folderPath);
			result = ResultForm.createSuccessResultForm(fileInfo, "查询成功");
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			result = ResultForm.createErrorResultForm(null, e.getMessage());
		}
		return result;
	}

	@RequestMapping("/getFiles")
	public Object getFiles(@RequestBody(required = false) JsonRequestBody jsonRequestBody) {
		String folderPath = jsonRequestBody.getString("folderPath");
		ResultForm<?> result = null;
		try {
			List<FileInfo> list = folderFileServiceImpl.getFileList(folderPath);
			result = ResultForm.createSuccessResultForm(list, "查询成功");
		} catch (IOException e) {
			e.printStackTrace();
			result = ResultForm.createErrorResultForm(null, e.getMessage());
		}
		return result;
	}

}
