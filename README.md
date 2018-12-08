# JBM 
[![输开源协议](https://img.shields.io/badge/License-Apache--2.0-brightgreen.svg "Apache")](https://www.apache.org/licenses/LICENSE-2.0)


- 由于框架诞生之初是初期期spring boot 1.0时代，所以有很多兼容性问题，在spring boot 2.0走红之后，JBM5.0全面升级融合个大主流中间件的starter 
- 4.x以前版本主要基于dubbo分布式体系，5.0将重心转移到spring cloud
- 基于java spring boot 模块化企业级框架
- 主要由util;autoconfig;webjars等模块组成
- 基于Spring Boot原则轻量级封装，目前版本还在迭代，追求的是稳定性可靠性
- 本框架基于IoT目标业务诞生，所以追求的开发效率和性能的平衡
- 称为模块化主要集成了多种工具的优势，通过Spring的有机结合，在基础框架的基础上快速构建企业自有的体系
- 框架所有内容通过分布式结构连接，通过Dubbo,Rest等方式相互调用


### 技术选型
-JDK:JDK1.8+
- 核心框架：Spring Boot
- 安全框架：Spring Security
- 任务调度：Spring + Quartz + Zookeeper
- 持久层框架：MyBatis + MyBatis-Plus + JPA + Spring Data
- 文档性架构：MongoDB + FastDFS
- 数据库连接池：Alibaba Druid
- 缓存框架：Redis + Guava
- 会话管理：Spring Session + Redis
- 日志管理：SLF4J+Log4j2+LogBack 日志收集
- 前端框架：Vue;Angular JS;Bootstrap;Jquery
 
[☆jbm-framework-dependencies](https://github.com/numen06/JBM/tree/master/jbm-framework-dependencies)为maven基础，标示框架内所有jar的版本，本框架已经消除大部分兼容性问题，为一次集成打下结实基础。
集成到项目的时候统一继承，即可实现JBM统一版本技术体系管理功能。

```
<parent>
    <groupId>com.jbm</groupId>
    <jbm-framework-sampleependencies</artifactId>
    <version>5.0.0-SNAPSHOT</version>
</parent>
```


[JBM操作MySql案例](https://github.com/numen06/jbm-framework-sample/tree/master/jbm-framework-sample-mysql)
通过集成

```
<dependency>
    <groupId>com.jbm</groupId>
    <artifactId>jbm-framework-micro-service</artifactId>
</dependency>
```

完成富有对数据操作的微服务功能，围绕针对实体操作的ORM架构体系，自动创建数据库表和对应的所有增删改查，简单的几行代码就能完成。


```
<dependency>
    <groupId>com.jbm</groupId>
    <artifactId>jbm-util</artifactId>
</dependency>
```
提升为framework等级，提升整体util实用程度，基于jodd和hutool轻量级封装，延续两个的版本生命力，提升使用开发力度
