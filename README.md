# Android Chat App

[English](./README.md) | [中文](./README_CN.md)

Real-time messaging application based on native Android development and Spring Boot backend, supporting cross-network message transmission.

## Architecture

- **Client**: Native Android application
- **Server**: Spring Boot RESTful API
- **Database**: MySQL 8.0+
- **Network**: Supports internal network penetration for public access

## Environment Requirements

| Tool | Version | Purpose |
|------|---------|---------|
| Android Studio | Latest | Client development/running |
| IntelliJ IDEA | Latest | Server development/running |
| MySQL | 8.0+ | Database |
| Oray Phsray | Latest | Internal network penetration tool (optional) |
| JDK | 1.8 | Java development environment |

## Quick Deployment

### 1. Database Configuration

1. Install MySQL 8.0+
2. Create a database named `chat_app`
3. Execute the SQL script file in the project to create table structures

### 2. Server Configuration

1. Open the `Serve/chat` directory using IntelliJ IDEA
2. Modify `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/chat_app?useSSL=false&serverTimezone=UTC
   spring.datasource.username=your_mysql_username
   spring.datasource.password=your_mysql_password
   
   # Modify to your static resource directory
   spring.web.resources.static-locations=file:///your_path/Chat_informations/post_images/
   ```
3. Create the following directory structure for storing user-uploaded images:
   ```
   E:/Chat_informations/
   └── post_images/
   ```
4. Run `ChatApplication.java` to start the server

### 3. Client Configuration

1. Open the `Client/chat` directory using Android Studio
2. Modify API address in `app/src/main/java/com/example/chat/utils/Constants.java`:
   ```java
   // For local testing use localhost:8080 or 10.0.2.2:8080 (emulator)
   // For LAN testing use internal IP like 192.168.x.x:8080
   // For public access use Oray Phsray domain
   public static final String BASE_URL = "http://your_ip_or_domain:8080/api";
   public static final String VERSION_BASE_URL = "http://your_ip_or_domain:8080/";
   ```
3. Build and run the application

### 4. Internal Network Penetration Setup (Optional)

For external network access:
1. Download and install [Oray Phsray](https://hsk.oray.com/)
2. Register and log in to your Phsray account
3. Add mapping, map local port 8080 to a public domain
4. Configure the domain provided by Phsray to `Constants.java` in the client

## Features

- User registration and login
- Friend addition and management
- Real-time chat
- Post creation and sharing
- Privacy and security settings

## Project Structure

```
Android Chat/
├── Client/ # Android client
│ └── chat/ # Android Studio project
└── Serve/ # Spring Boot server
└── chat/ # Spring Boot project
```


## Tech Stack

- **Client**:
  - Android SDK (Min SDK 24, Target SDK 34)
  - Retrofit2 + OkHttp3 (Network requests)
  - Material Design
  - Glide (Image loading)

- **Server**:
  - Spring Boot
  - Spring Data JPA
  - MySQL
  - RESTful API

## Common Issues

1. Database connection failure: Check if MySQL service is running and connection parameters are correct
2. Client cannot connect to server: Check network configuration, confirm API address is correct, server is running normally
3. Images not displaying: Ensure static resource directory is configured correctly and has read/write permissions

## Contact Information

For any questions, please contact:

- QQ: 1652855974
- WeChat: Lgy2873551074
