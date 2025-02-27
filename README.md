# -Android-app-chat

安卓原生开发-聊天app

使用软件,IntelliJ IDEA(版本无所谓)、Android studio(版本无所谓)、花生壳

该项目将spring boot部署在本地作为服务器，接收每个app的请求，实现随时随地发送接收信息。

由于能够不限地点发送消息，需要使用第三方工具实现内网穿透，将本地的内网ip映射到工具提供的公网域名，这里使用的是花生壳内网穿透(免费)
下载地址：https://hsk.oray.com    
使用教程：https://service.oray.com/question/15507.html

在IDEA中打开spring boot项目Serve，下载相应的依赖，运行后默认监听端口为8080，在花生壳内网穿透编辑页面将这个端口和自己的内网ip地址填入，实现内网穿透。
映射完成后，将客户端Client文件的java/com/example/chat/utils/Constants.java文件中的BASE_URL和VERSION_BASE_URL修改为自己得到的域名(注：BASE_URL结尾的/api不要误删)

服务端Serve中src/main/resources/application.properties文件里的spring.web.resources.static-locations为用户上传的头像等图片的保存位置
