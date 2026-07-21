package com.jbm.cluster.center.controller;

import cn.hutool.core.util.StrUtil;
import com.jbm.cluster.api.entitys.basic.BaseDic;
import com.jbm.cluster.center.service.BaseDicService;
import com.jbm.cluster.center.service.DictionaryI18nService;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.mvc.web.MasterDataTreeCollection;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @Author: wesley.zhang
 * @Create: 2020-02-25 03:47:52
 */
@Api(tags = "系统字典")
@RestController
@RequestMapping("/baseDic")
public class BaseDicController extends MasterDataTreeCollection<BaseDic, BaseDicService> {

    private final DictionaryI18nService dictionaryI18nService;

    public BaseDicController(DictionaryI18nService dictionaryI18nService) {
        this.dictionaryI18nService = dictionaryI18nService;
    }

    @ApiOperation("获取数据字典")
    @GetMapping("/getDicMap")
    public ResultBody<Map<String, List<BaseDic>>> getDicMap(
            Locale locale,
            @RequestHeader(value = "content-language", required = false) String contentLanguage) {
        Locale requestedLocale = resolveLocale(contentLanguage, locale);
        Map<String, List<BaseDic>> result = dictionaryI18nService.localize(this.service.getDicMap(), requestedLocale);
        return ResultBody.success(result, "获取数据字典成功");
    }

    static Locale resolveLocale(String contentLanguage, Locale acceptLanguageLocale) {
        if (StrUtil.isBlank(contentLanguage)) {
            return acceptLanguageLocale == null ? Locale.CHINESE : acceptLanguageLocale;
        }
        String languageTag = StrUtil.subBefore(contentLanguage, ',', false).trim().replace('_', '-');
        Locale locale = Locale.forLanguageTag(languageTag);
        return StrUtil.isBlank(locale.getLanguage())
                ? (acceptLanguageLocale == null ? Locale.CHINESE : acceptLanguageLocale)
                : locale;
    }


}
