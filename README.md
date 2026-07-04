# mugsun-boot

Mugsun 平台单体版：将全部功能模块打包为单个可执行应用，开箱即用的企业级快速开发平台。基于 [mugsun-core](https://github.com/curdx/mugsun-core) 构建。

## 功能

组织用户与角色权限、菜单与按钮权限、行级数据权限、字典与参数、多租户 SaaS、附件与多云对象存储、短信、通知公告、操作日志与数据审计、工作流、分布式任务调度、可视化代码生成、报表。

## 技术栈

JDK 21（虚拟线程）· Spring Boot 3.5.x · MyBatis-Flex · Sa-Token · JetCache · Warm-Flow · PowerJob · Knife4j

## 运行

1. 先在 mugsun-core 执行 `mvn clean install -DskipTests`
2. 准备 MySQL、Redis

```bash
mvn clean package -DskipTests
```

## 许可

[Apache License 2.0](LICENSE)
