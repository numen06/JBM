package com.jbm.cluster.doc;

import com.jbm.autoconfig.dic.annotation.EnableJbmDictionary;
import com.jbm.cluster.api.constants.OrgType;
import com.jbm.cluster.api.entitys.doc.BaseDoc;
import com.jbm.cluster.doc.mapper.BaseDocMapper;
import com.jbm.framework.masterdata.code.annotation.EnableCodeAutoGeneate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;


/**
 * 平台文档服务
 * 提供文档的增删下载
 *
 * @author wesley.zhang
 */
@EnableFeignClients
@SpringBootApplication
@EnableDiscoveryClient
//实体扫描路径
@EntityScan(basePackages = {"com.jbm.cluster.api.entitys"})
//MyBatis Mapper扫描路径
@MapperScan(basePackageClasses = BaseDocMapper.class)
//启用JBM字典功能
@EnableJbmDictionary(basePackageClasses = OrgType.class)
//启用自动生成代码功能
@EnableCodeAutoGeneate(entityPackageClasses = {BaseDoc.class}, targetPackage = "com.jbm.cluster.doc")
public class JbmDocApplication {

    public static void main(String[] args) {
        SpringApplication.run(JbmDocApplication.class, args);
    }


}
