package com.jbm.cluster.job.util;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.id.SaIdUtil;
import cn.dev33.satoken.oauth2.logic.SaOAuth2Template;
import cn.dev33.satoken.oauth2.model.ClientTokenModel;
import cn.dev33.satoken.oauth2.model.SaClientModel;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.jbm.cluster.api.entitys.job.SysJob;
import com.jbm.cluster.core.constant.JbmSecurityConstants;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.http.HttpMethod;

/**
 * 定时任务处理（允许并发执行）
 *
 * @author wesley
 */
@Slf4j
public class QuartzJobExecution extends AbstractQuartzJob {





}
