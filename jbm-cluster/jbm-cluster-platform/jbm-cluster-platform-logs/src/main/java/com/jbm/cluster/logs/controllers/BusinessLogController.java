package com.jbm.cluster.logs.controllers;

import com.jbm.cluster.api.entitys.log.BusinessLog;
import com.jbm.cluster.api.entitys.log.BusinessLogStageSnapshot;
import com.jbm.cluster.api.form.log.AppendBusinessLogForm;
import com.jbm.cluster.api.form.log.BusinessLogForm;
import com.jbm.cluster.api.form.log.BusinessLogStageUpdateForm;
import com.jbm.cluster.api.form.log.CreateBusinessLogForm;
import com.jbm.cluster.api.form.log.InitBusinessLogStageForm;
import com.jbm.cluster.logs.service.BusinessLogService;
import com.jbm.cluster.logs.service.DemoBusinessLogService;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.usage.paging.DataPaging;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 业务日志控制器
 * 提供业务日志的创建、追加、查询等功能
 * 基于OpenObserve实现日志存储，支持自定义过期时间
 * 
 * ⚠️ 过期管理说明：
 * - 过期管理已托管给OpenObserve（通过流的保留策略TTL自动过期）
 * - 日志根据过期时间自动分组到不同的流中（business_log_7d, business_log_30d等）
 * - 过期数据由OpenObserve自动删除，无需手动清理
 * 
 * @author wesley
 */
@Slf4j
@Api(tags = "业务日志接口")
@RestController
@RequestMapping("/businessLog")
public class BusinessLogController {
    
    @Autowired
    private BusinessLogService businessLogService;
    
    /**
     * 创建业务日志
     * 生成唯一的logId用于后续追加和查询
     * 
     * 支持两种使用方式：
     * 1. 原有格式 - 设置 module、operation、userId 等字段
     * 2. 集成模块格式 - 设置 businessType、businessId、source 等字段
     * 
     * @param form 创建业务日志表单（统一使用 CreateBusinessLogForm）
     * @return 返回生成的logId
     */
    @ApiOperation(value = "创建业务日志", notes = "生成业务日志ID，用于后续追加内容")
    @PostMapping("/create")
    public ResultBody<Map<String, String>> createLog(@RequestBody CreateBusinessLogForm form) {
        try {
            String logId = businessLogService.createLog(form);
            Map<String, String> result = new HashMap<>();
            result.put("logId", logId);
            return ResultBody.success(result, "创建业务日志成功");
        } catch (Exception e) {
            return ResultBody.error(null, "创建业务日志失败", e);
        }
    }
    
    /**
     * 追加日志内容
     * 根据logId追加新的日志内容
     * 
     * @param form 追加日志表单
     * @return 是否追加成功
     */
    @ApiOperation(value = "追加日志内容", notes = "根据logId追加新的日志内容，传递完整的AppendBusinessLogForm对象")
    @PostMapping("/append")
    public ResultBody<Boolean> appendLog(@Validated @RequestBody AppendBusinessLogForm form) {
        try {
            boolean success = businessLogService.appendLog(form);
            return ResultBody.success(success, success ? "追加日志成功" : "追加日志失败");
        } catch (Exception e) {
            return ResultBody.error(false, "追加日志失败", e);
        }
    }
    
    /**
     * 追加日志内容（简化版，供 Feign 客户端使用）
     * 通过路径参数 logId + 请求体 content
     * 
     * @param logId 日志ID
     * @param content 日志内容
     * @return 是否追加成功
     */
    @ApiOperation(value = "追加日志内容（简化版）", notes = "供Feign客户端使用的简化接口，通过路径参数logId+字符串content")
    @PostMapping("/append/{logId}")
    public ResultBody<Boolean> appendLogSimple(
            @ApiParam(value = "业务日志ID", required = true) @PathVariable String logId,
            @RequestBody String content) {
        try {
            AppendBusinessLogForm form = new AppendBusinessLogForm();
            form.setLogId(logId);
            form.setContent(content);
            return appendLog(form);
        } catch (Exception e) {
            return ResultBody.error(false, "追加日志失败", e);
        }
    }
    
    /**
     * 统一查询业务日志接口
     * 通过format参数控制返回格式：
     * - multiline: 返回多行格式（List<BusinessLog>）
     * - full: 返回完整内容（String，JSON格式）
     * - range: 按行号范围查询（需要startLine和endLine参数）
     * 
     * @param logId 业务日志ID
     * @param format 返回格式：multiline（多行格式）、full（完整内容）、range（行号范围），默认为multiline
     * @param formatted 是否格式化（仅当format=full时有效，true: 添加头部信息和行号，false: 仅返回原始日志内容）
     * @param startLine 起始行号（从1开始，仅当format=range时有效）
     * @param endLine 结束行号（-1表示到最后一行，仅当format=range时有效）
     * @return 根据format返回不同格式的数据
     */
    @ApiOperation(value = "查询业务日志（统一接口）", notes = "通过format参数控制返回格式：multiline（多行格式）、full（完整内容）、range（行号范围）")
    @GetMapping("/get/{logId}")
    public ResultBody<?> getLog(
            @ApiParam(value = "业务日志ID", required = true) @PathVariable String logId,
            @ApiParam(value = "返回格式：multiline（多行格式）、full（完整内容）、range（行号范围）", required = false) 
            @RequestParam(required = false, defaultValue = "multiline") String format,
            @ApiParam(value = "是否格式化（仅当format=full时有效）", required = false) 
            @RequestParam(required = false, defaultValue = "true") Boolean formatted,
            @ApiParam(value = "起始行号（仅当format=range时有效，从1开始）", required = false) 
            @RequestParam(required = false, defaultValue = "1") Integer startLine,
            @ApiParam(value = "结束行号（仅当format=range时有效，-1表示到最后一行）", required = false) 
            @RequestParam(required = false, defaultValue = "-1") Integer endLine) {
        try {
            switch (format.toLowerCase()) {
                case "full":
                    String content = businessLogService.getLogByIdFullContent(logId, formatted);
                    return ResultBody.success(content, "查询业务日志成功");
                case "range":
                    List<BusinessLog> rangeLogs = businessLogService.getLogByLineRange(logId, startLine, endLine);
                    return ResultBody.success(rangeLogs, "查询业务日志成功");
                case "multiline":
                default:
                    List<BusinessLog> logs = businessLogService.getLogByIdMultiLine(logId);
                    return ResultBody.success(logs, "查询业务日志成功");
            }
        } catch (Exception e) {
            return ResultBody.error(null, "查询业务日志失败", e);
        }
    }

    /**
     * 获取日志总行数
     * 
     * @param logId 业务日志ID
     * @return 总行数
     */
    @ApiOperation(value = "获取日志总行数", notes = "返回指定logId的日志总行数")
    @GetMapping("/get/{logId}/lines")
    public ResultBody<Integer> getLogTotalLines(
            @ApiParam(value = "业务日志ID", required = true) @PathVariable String logId) {
        try {
            Integer totalLines = businessLogService.getLogTotalLines(logId);
            return ResultBody.success(totalLines, "获取总行数成功");
        } catch (Exception e) {
            return ResultBody.error(0, "获取总行数失败", e);
        }
    }

    /**
     * 浏览器预览日志文件（直接返回文本格式）
     * 用于浏览器直接打开.log文件
     * 
     * @param logId 业务日志ID
     * @param formatted 是否格式化（true: 添加头部信息和行号，false: 仅返回原始日志内容）
     * @param response HTTP响应
     */
    @ApiOperation(value = "浏览器预览日志文件", notes = "返回指定logId的日志内容，用于浏览器直接预览。默认返回原始格式（不格式化）")
    @GetMapping("/get/{logId}.log")
    public void getLogFile(
            @ApiParam(value = "业务日志ID", required = true) @PathVariable String logId,
            @ApiParam(value = "是否格式化（true: 添加头部信息和行号，false: 仅返回原始日志内容）", required = false) 
            @RequestParam(required = false, defaultValue = "false") Boolean formatted,
            HttpServletResponse response) throws IOException {
        try {
            String logContent = businessLogService.getLogByIdFullContent(logId, formatted);
            
            response.setContentType("text/plain; charset=utf-8");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "inline; filename=\"" + 
                    java.net.URLEncoder.encode(logId + ".log", "UTF-8") + "\"");
            
            PrintWriter writer = response.getWriter();
            writer.write(logContent);
            writer.flush();
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("text/plain; charset=utf-8");
            response.getWriter().write("获取日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 分页查询业务日志
     * 支持多条件组合查询
     * 
     * @param form 查询表单
     * @return 分页结果
     */
    @ApiOperation(value = "分页查询业务日志", notes = "支持多条件组合查询业务日志")
    @PostMapping("/query")
    public ResultBody<DataPaging<BusinessLog>> queryLogs(@RequestBody(required = false) BusinessLogForm form) {
        try {
            if (form == null) {
                form = new BusinessLogForm();
            }
            DataPaging<BusinessLog> dataPaging = businessLogService.queryLogs(form);
            return ResultBody.success(dataPaging, "查询业务日志成功");
        } catch (Exception e) {
            return ResultBody.error(null, "查询业务日志失败", e);
        }
    }
    
    /**
     * 更新日志过期时间（限制使用）
     * 
     * ⚠️ 注意：由于日志存储在OpenObserve的不同流中（根据过期时间分组），
     * 更新过期时间会导致日志迁移到新的流。当前实现仅标记新的过期时间，
     * 原始日志仍在原流中，直到原流的TTL到期后自动删除。
     * 
     * 建议：在创建日志时设置正确的过期时间，避免后续修改。
     * 
     * @param logId 业务日志ID
     * @param expireDays 过期天数
     * @return 是否更新成功
     */
    @ApiOperation(value = "更新日志过期时间", notes = "⚠️ 限制使用：由于日志按过期时间存储在不同流中，更新过期时间不会立即迁移数据。建议在创建时设置正确的过期时间。")
    @PutMapping("/updateExpireTime/{logId}/{expireDays}")
    public ResultBody<Boolean> updateExpireTime(
            @ApiParam(value = "业务日志ID", required = true) @PathVariable String logId,
            @ApiParam(value = "过期天数", required = true) @PathVariable Integer expireDays) {
        try {
            boolean success = businessLogService.updateExpireTime(logId, expireDays);
            if (success) {
                return ResultBody.success(true, 
                        "已标记新的过期时间。注意：原始日志仍在原流中，将在原流TTL到期后自动删除。");
            } else {
                return ResultBody.error(false, "更新过期时间失败");
            }
        } catch (Exception e) {
            return ResultBody.error(false, "更新过期时间失败", e);
        }
    }
    
    /**
     * 生成日志的临时访问URL（类似OSS签名URL）
     * 
     * @param logId 业务日志ID
     * @param expireMinutes 过期时间（分钟），默认60分钟
     * @param baseUrl 基础URL（可选，如果不传则自动从请求中识别）
     * @return 临时访问URL
     */
    @ApiOperation(value = "生成日志临时访问URL", notes = "生成类似OSS签名URL的临时访问链接，带过期时间。baseUrl参数可选，如果不传则自动从请求中识别")
    @GetMapping("/generateUrl/{logId}")
    public ResultBody<Map<String, String>> generateTemporaryUrl(
            @ApiParam(value = "业务日志ID", required = true) @PathVariable String logId,
            @ApiParam(value = "过期时间（分钟）", required = false) @RequestParam(required = false, defaultValue = "60") Integer expireMinutes,
            @ApiParam(value = "基础URL（可选，如果不传则自动从请求中识别）", required = false) @RequestParam(required = false) String baseUrl,
            HttpServletRequest request) {
        try {
            // 如果baseUrl未传，从请求中自动识别
            String serverBaseUrl = baseUrl;
            if (serverBaseUrl == null || serverBaseUrl.isEmpty()) {
                String scheme = request.getScheme();
                String serverName = request.getServerName();
                int serverPort = request.getServerPort();
                String contextPath = request.getContextPath();
                serverBaseUrl = scheme + "://" + serverName + (serverPort != 80 && serverPort != 443 ? ":" + serverPort : "") + contextPath;
            }
            
            // 生成临时URL参数
            Map<String, String> urlParams = businessLogService.generateTemporaryUrlParams(logId, expireMinutes);
            
            // 构建完整URL（类似OSS格式）
            String fileName = urlParams.get("fileName");
            String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8");
            String tempUrl = String.format("%s/businessLog/download/%s?Expires=%s&OSSAccessKeyId=%s&Signature=%s",
                    serverBaseUrl, encodedFileName, 
                    urlParams.get("expires"), 
                    urlParams.get("accessKeyId"), 
                    urlParams.get("signature"));
            
            Map<String, String> result = new HashMap<>();
            result.put("url", tempUrl);
            result.put("logId", logId);
            result.put("expireMinutes", String.valueOf(expireMinutes));
            result.put("expires", urlParams.get("expires"));
            result.put("accessKeyId", urlParams.get("accessKeyId"));
            result.put("signature", urlParams.get("signature"));
            
            return ResultBody.success(result, "生成临时访问URL成功");
        } catch (Exception e) {
            return ResultBody.error(null, "生成临时访问URL失败", e);
        }
    }
    
    /**
     * 通过临时URL下载日志（类似OSS的下载接口）
     * 支持通过Expires、OSSAccessKeyId、Signature参数验证
     * 
     * @param logId 业务日志ID（从文件名解析：raw-{logId}.log）
     * @param expires 过期时间戳
     * @param ossAccessKeyId AccessKeyId
     * @param signature 签名
     * @param response HTTP响应
     */
    @ApiOperation(value = "通过临时URL下载日志", notes = "类似OSS的下载接口，通过签名验证后返回日志内容")
    @GetMapping("/download/raw-{logId}.log")
    public void downloadLogByUrl(
            @ApiParam(value = "业务日志ID", required = true) @PathVariable String logId,
            @ApiParam(value = "过期时间戳") @RequestParam(required = false) Long expires,
            @ApiParam(value = "AccessKeyId") @RequestParam(required = false) String ossAccessKeyId,
            @ApiParam(value = "签名") @RequestParam(required = false) String signature,
            HttpServletResponse response) {
        try {
            // 验证过期时间
            if (expires != null && expires > 0) {
                long currentTime = System.currentTimeMillis() / 1000;
                if (currentTime > expires) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("URL已过期");
                    return;
                }
            }
            
            // 验证签名（简化版，实际应该验证signature）
            // 这里直接从token中获取logId并返回日志内容
            
            // 获取日志内容（下载接口返回原始格式，不格式化）
            String logContent = businessLogService.getLogByIdFullContent(logId, false);
            
            // 设置响应头
            response.setContentType("text/plain; charset=utf-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + 
                    java.net.URLEncoder.encode("raw-" + logId + ".log", "UTF-8") + "\"");
            response.setHeader("Content-Length", String.valueOf(logContent.getBytes("UTF-8").length));
            
            // 写入日志内容
            PrintWriter writer = response.getWriter();
            writer.write(logContent);
            writer.flush();
            
        } catch (Exception e) {
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("获取日志失败: " + e.getMessage());
            } catch (IOException ioException) {
                // 忽略
            }
        }
    }
    
    /**
     * 通过token访问日志（简化版）
     * 
     * @param logId 业务日志ID
     * @param token 临时访问token
     * @return 日志内容
     */
    @ApiOperation(value = "通过token访问日志", notes = "使用临时token访问日志内容")
    @GetMapping("/access/{logId}")
    public ResultBody<String> getLogByToken(
            @ApiParam(value = "业务日志ID", required = true) @PathVariable String logId,
            @ApiParam(value = "临时访问token", required = true) @RequestParam String token) {
        try {
            String logContent = businessLogService.getLogByToken(logId, token);
            return ResultBody.success(logContent, "获取日志成功");
        } catch (Exception e) {
            return ResultBody.error(null, "获取日志失败: " + e.getMessage(), e);
        }
    }

    /**
     * SSE实时推送业务日志
     * 
     * @param logId 业务日志ID
     * @param intervalMillis 轮询间隔（毫秒）
     * @return SseEmitter
     */
    @ApiOperation(value = "实时监听业务日志", notes = "使用SSE实时推送指定logId的最新日志行")
    @GetMapping(value = "/stream/{logId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamBusinessLog(
            @ApiParam(value = "业务日志ID", required = true) @PathVariable String logId,
            @ApiParam(value = "轮询间隔（毫秒）", required = false, defaultValue = "2000") @RequestParam(required = false, defaultValue = "2000") Long intervalMillis) {

        long safeInterval = Math.max(500L, Math.min(intervalMillis == null ? 2000L : intervalMillis, 5000L));
        SseEmitter emitter = new SseEmitter(0L);
        AtomicBoolean active = new AtomicBoolean(true);
        long[] stageVersionHolder = new long[]{-1L};

        emitter.onCompletion(() -> active.set(false));
        emitter.onTimeout(() -> {
            active.set(false);
            emitter.complete();
        });
        emitter.onError(e -> active.set(false));

        CompletableFuture.runAsync(() -> {
            int nextLine = 1;
            boolean logReady = false;
            try {
                emitter.send(SseEmitter.event().name("status").data(
                        buildStatusPayload("CONNECTED", "等待业务日志写入...")));
                pushStageSnapshot(emitter, logId, stageVersionHolder);
            } catch (IOException ioException) {
                log.debug("SSE连接已断开: logId={}", logId);
                active.set(false);
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                    // ignore
                }
                return;
            }

            while (active.get()) {
                try {
                    Integer totalLines = businessLogService.getLogTotalLines(logId);
                    if (totalLines == null || totalLines == 0) {
                        Thread.sleep(safeInterval);
                        continue;
                    }

                    if (!logReady) {
                        logReady = true;
                        nextLine = Math.max(1, totalLines - 200 + 1);
                        try {
                            emitter.send(SseEmitter.event().name("status").data(
                                    buildStatusPayload("READY", "已找到业务日志，开始推送最新内容")));
                        } catch (IOException e) {
                            log.debug("SSE连接已断开: logId={}", logId);
                            active.set(false);
                            break;
                        }
                    }

                    List<BusinessLog> newLogs = businessLogService.getLogByLineRange(logId, nextLine, -1);
                    if (!CollectionUtils.isEmpty(newLogs)) {
                        try {
                            emitter.send(SseEmitter.event().name("log").data(newLogs));
                        } catch (IOException e) {
                            log.debug("SSE连接已断开: logId={}", logId);
                            active.set(false);
                            break;
                        }
                        BusinessLog last = newLogs.get(newLogs.size() - 1);
                        Integer lineNumber = last.getLineNumber();
                        if (lineNumber != null && lineNumber > 0) {
                            nextLine = lineNumber + 1;
                        } else {
                            nextLine += newLogs.size();
                        }
                    } else {
                        Thread.sleep(safeInterval);
                    }
                    
                    pushStageSnapshot(emitter, logId, stageVersionHolder);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.debug("SSE轮询被中断: logId={}", logId);
                    active.set(false);
                    break;
                } catch (Exception e) {
                    log.error("SSE推送异常: logId={}", logId, e);
                    try {
                        emitter.send(SseEmitter.event().name("error").data(
                                buildStatusPayload("ERROR", e.getMessage())));
                    } catch (IOException ioException) {
                        log.debug("SSE连接已断开，无法发送错误消息: logId={}", logId);
                        active.set(false);
                        break;
                    }
                    // 对于业务异常，不立即关闭连接，继续尝试
                    try {
                        Thread.sleep(safeInterval);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        active.set(false);
                        break;
                    }
                }
            }
            
            // 优雅关闭连接
            try {
                if (active.get()) {
                    emitter.send(SseEmitter.event().name("status").data(
                            buildStatusPayload("CLOSED", "连接已关闭")));
                }
            } catch (Exception ignored) {
                // ignore
            } finally {
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                    // ignore
                }
            }
        });

        return emitter;
    }

    @ApiOperation(value = "初始化业务日志阶段", notes = "配置阶段列表，用于展示阶段进度")
    @PostMapping("/stage/init")
    public ResultBody<BusinessLogStageSnapshot> initStages(@Validated @RequestBody InitBusinessLogStageForm form) {
        try {
            BusinessLogStageSnapshot snapshot = businessLogService.initStages(form);
            return ResultBody.success(snapshot, "初始化阶段成功");
        } catch (Exception e) {
            return ResultBody.error(null, "初始化阶段失败", e);
        }
    }

    @ApiOperation(value = "更新业务日志阶段进度", notes = "阶段状态、进度变更时调用")
    @PostMapping("/stage/update")
    public ResultBody<BusinessLogStageSnapshot> updateStage(@Validated @RequestBody BusinessLogStageUpdateForm form) {
        try {
            BusinessLogStageSnapshot snapshot = businessLogService.updateStage(form);
            return ResultBody.success(snapshot, "更新阶段成功");
        } catch (Exception e) {
            return ResultBody.error(null, "更新阶段失败", e);
        }
    }

    @ApiOperation(value = "查询业务日志阶段快照", notes = "用于前端初始展示当前阶段状态")
    @GetMapping("/stage/{logId}")
    public ResultBody<BusinessLogStageSnapshot> getStageSnapshot(
            @ApiParam(value = "业务日志ID", required = true) @PathVariable String logId) {
        try {
            BusinessLogStageSnapshot snapshot = businessLogService.getStageSnapshot(logId);
            return ResultBody.success(snapshot, "查询阶段快照成功");
        } catch (Exception e) {
            return ResultBody.error(null, "查询阶段快照失败", e);
        }
    }

    @Autowired
    private DemoBusinessLogService demoBusinessLogService;

    /**
     * 创建演示用例
     * 快速创建一个演示日志，使用JbmBusinessLogTemplate模拟真实业务场景
     * 
     * @param mode 演示模式：simple（简单日志）、single-stage（单阶段进度跟踪）、multi-stage（多阶段进度跟踪）
     * @return 返回生成的logId
     */
    @ApiOperation(value = "创建演示用例", notes = "快速创建一个演示日志，支持三种模式：simple（简单日志）、single-stage（单阶段进度跟踪）、multi-stage（多阶段进度跟踪）")
    @PostMapping("/demo")
    public ResultBody<Map<String, String>> createDemo(
            @ApiParam(value = "演示模式：simple（简单日志）、single-stage（单阶段进度跟踪）、multi-stage（多阶段进度跟踪）", required = false) 
            @RequestParam(required = false, defaultValue = "multi-stage") String mode) {
        try {
            CreateBusinessLogForm form = new CreateBusinessLogForm();
            form.setModule("DEMO");
            form.setOperation("DEMO_" + mode.toUpperCase());
            form.setUsername("demo");
            form.setUserId("demo");
            form.setAutoTimestamp(true);
            
            String logId = businessLogService.createLog(form);

            // 根据模式异步执行不同的演示任务
            CompletableFuture.runAsync(() -> {
                switch (mode.toLowerCase()) {
                    case "simple":
                        demoBusinessLogService.executeSimpleDemo(logId);
                        break;
                    case "single-stage":
                        demoBusinessLogService.executeSingleStageDemo(logId);
                        break;
                    case "multi-stage":
                    default:
                        demoBusinessLogService.executeMultiStageDemo(logId);
                        break;
                }
            });

            Map<String, String> response = new HashMap<>();
            response.put("logId", logId);
            response.put("module", "DEMO");
            response.put("mode", mode);
            return ResultBody.success(response, "演示任务创建成功，logId已返回");
        } catch (Exception e) {
            return ResultBody.error(null, "创建演示任务失败", e);
        }
    }
    
    /**
     * 创建阶段演示用例（兼容旧接口）
     * 
     * @return 返回生成的logId
     */
    @ApiOperation(value = "创建阶段演示用例（兼容接口）", notes = "快速创建一个多阶段演示日志，使用JbmBusinessLogTemplate模拟真实业务场景")
    @PostMapping("/demo/stage")
    public ResultBody<Map<String, String>> createStageDemo() {
        return createDemo("multi-stage");
    }

    private Map<String, String> buildStatusPayload(String status, String message) {
        Map<String, String> payload = new HashMap<>();
        payload.put("status", status);
        payload.put("message", message);
        return payload;
    }

    private void pushStageSnapshot(SseEmitter emitter, String logId, long[] versionHolder) {
        try {
            BusinessLogStageSnapshot snapshot = businessLogService.getStageSnapshot(logId);
            if (snapshot == null) {
                return;
            }
            long snapshotVersion = snapshot.getVersion() != null ? snapshot.getVersion() : 0L;
            if (versionHolder[0] >= snapshotVersion) {
                return;
            }
            emitter.send(SseEmitter.event().name("progress").data(snapshot));
            versionHolder[0] = snapshotVersion;
        } catch (IOException e) {
            // 连接已断开，忽略推送失败
            log.debug("推送阶段快照时连接已断开: logId={}", logId);
        } catch (Exception e) {
            // 其他异常记录日志但不中断主流程
            log.warn("推送阶段快照异常: logId={}", logId, e);
        }
    }
}

