# Android 聊天应用

[English](./README.md) | [中文](./README_CN.md)

基于Android原生开发与Spring Boot后端的即时通讯应用，支持跨网络实时消息传输。

## 项目架构

- **客户端**：Android原生应用
- **服务端**：Spring Boot RESTful API
- **数据库**：MySQL 8.0+
- **网络**：支持内网穿透实现公网访问

## 环境要求

| 工具 | 版本 | 用途 |
|------|------|------|
| Android Studio | 最新版 | 客户端开发/运行 |
| IntelliJ IDEA | 最新版 | 服务端开发/运行 |
| MySQL | 8.0+ | 数据库 |
| 花生壳 | 最新版 | 内网穿透工具(可选) |
| JDK | 1.8 | Java开发环境 |

## 快速部署

### 1. 数据库配置

1. 安装MySQL 8.0+
2. 创建名为`chat_app`的数据库
3. 执行项目中的SQL脚本文件创建表结构

### 2. 服务端配置

1. 使用IntelliJ IDEA打开`Serve/chat`目录
2. 修改`src/main/resources/application.properties`：
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/chat_app?useSSL=false&serverTimezone=UTC
   spring.datasource.username=你的MySQL用户名
   spring.datasource.password=你的MySQL密码
   
   # 修改为你的静态资源目录
   spring.web.resources.static-locations=file:///你的路径/Chat_informations/post_images/
   ```
3. 创建以下目录结构用于存储用户上传的图片：
   ```
   E:/Chat_informations/
   └── post_images/
   ```
4. 运行`ChatApplication.java`启动服务器

### 3. 客户端配置

1. 使用Android Studio打开`Client/chat`目录
2. 修改`app/src/main/java/com/example/chat/utils/Constants.java`中的API地址：
   ```java
   // 本地测试用localhost:8080或10.0.2.2:8080(模拟器)
   // 局域网测试使用内网IP如192.168.x.x:8080
   // 公网访问使用花生壳域名
   public static final String BASE_URL = "http://你的IP或域名:8080/api";
   public static final String VERSION_BASE_URL = "http://你的IP或域名:8080/";
   ```
3. 构建并运行应用

### 4. 内网穿透设置(可选)

如需从外网访问应用:
1. 下载并安装[花生壳](https://hsk.oray.com/)
2. 注册并登录花生壳账号
3. 添加映射，将本地8080端口映射到公网域名
4. 将花生壳提供的域名配置到客户端的`Constants.java`

## 功能特性

- 用户注册与登录
- 好友添加与管理
- 实时聊天
- 发布动态/帖子
- 隐私与安全设置

## 项目结构
```
Android Chat/
├── Client/ # 安卓客户端
│ └── chat/ # Android Studio项目
└── Serve/ # Spring Boot服务端
└── chat/ # Spring Boot项目
```


## 技术栈

- **客户端**:
  - Android SDK (Min SDK 24, Target SDK 34)
  - Retrofit2 + OkHttp3 (网络请求)
  - Material Design
  - Glide (图片加载)

- **服务端**:
  - Spring Boot
  - Spring Data JPA
  - MySQL
  - RESTful API

## 常见问题

1. 数据库连接失败：检查MySQL服务是否启动及连接参数是否正确
2. 客户端连接服务器失败：检查网络配置，确认API地址正确，服务器运行正常
3. 图片资源不显示：确认静态资源目录配置正确并有读写权限

## 联系方式

有任何问题请通过以下方式联系:

- QQ: 1652855974
- 微信: Lgy2873551074
