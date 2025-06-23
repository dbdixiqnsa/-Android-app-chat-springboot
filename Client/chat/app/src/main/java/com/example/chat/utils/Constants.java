package com.example.chat.utils;

public class Constants {
    // 服务器基础 URL
    public static final String BASE_URL = "http://192.168.193.163:8080/api";

    // 确保 BASE_URL_FRIENDS 以单个 '/' 结尾
    public static final String BASE_URL_FRIENDS = BASE_URL.endsWith("/") ? BASE_URL : BASE_URL + "/";

    // 添加版本验证 URL
    public static final String VERSION_BASE_URL = "http://192.168.193.163:8080/";
    public static final String VERSION_VERIFY_URL = VERSION_BASE_URL + "api/users/verify-version";

    // 添加当前客户端版本号
    public static final String CLIENT_VERSION = "1.0.1";

    // 用户相关 API
    public static final String LOGIN_URL = BASE_URL + "/users/login";
    public static final String REGISTER_URL = BASE_URL + "/users/register";
    public static final String USER_DETAILS_URL = BASE_URL + "/users/details";
    public static final String UPDATE_NICKNAME_URL = BASE_URL + "/users/update-nickname";
    public static final String UPDATE_USERNAME_URL = BASE_URL + "/users/update-username";
    public static final String UPDATE_PHOTO_URL = BASE_URL + "/users/update-photo";
    public static final String UPDATE_EMAIL_URL = BASE_URL + "/users/update-email";
    public static final String VERIFY_PASSWORD_URL = BASE_URL + "/users/verify-password";
    public static final String UPDATE_PASSWORD_URL = BASE_URL + "/users/update-password";
    public static final String ADD_SECURITY_URL = BASE_URL + "/users/add-security";
    public static final String SECURITY_QUESTIONS_URL = BASE_URL + "/users/security-questions";
    public static final String VERIFY_SECURITY_ANSWERS_URL = BASE_URL + "/users/verify-security-answers";
    public static final String FETCH_SECURITY_QUESTIONS_URL = BASE_URL + "/users/get-security-questions";
    public static final String VALIDATE_SECURITY_ANSWERS_URL = BASE_URL + "/users/validate-security-answers";
    public static final String DELETE_FRIEND_URL = BASE_URL + "/users/friends/delete";

    // 用户头像 URL
    public static final String USER_PHOTO_BASE_URL = BASE_URL_FRIENDS + "users/user-photo/";
    // 当前用户名，在登录后赋值
    public static String CURRENT_USERNAME = "";
}