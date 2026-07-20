package com.jbm.cluster.center.mapper;

import com.jbm.cluster.center.model.DictionaryMessage;
import com.jbm.framework.masterdata.annotation.MapperRepository;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 按语言批量读取已配置的字典国际化文案。
 */
@MapperRepository
public interface DictionaryMessageMapper {

    @Select("<script>" +
            " select code, message_text as messageText, locale " +
            " from prompt_message " +
            " where code like 'dict.%' " +
            " and locale in " +
            " <foreach collection='locales' item='locale' open='(' separator=',' close=')'>" +
            "   #{locale}" +
            " </foreach>" +
            "</script>")
    List<DictionaryMessage> selectByLocales(@Param("locales") Collection<String> locales);
}
