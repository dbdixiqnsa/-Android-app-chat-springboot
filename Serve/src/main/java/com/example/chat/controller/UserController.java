package com.example.chat.controller;

import com.example.chat.config.AppConfig;
import com.example.chat.dto.FriendRequestDTO;
import com.example.chat.dto.SecurityAnswerDTO;
import com.example.chat.dto.SecurityAnswerRequest;
import com.example.chat.dto.UserDTO;
import com.example.chat.model.*;
import com.example.chat.repository.FriendRemarkRepository;
import com.example.chat.repository.FriendRepository;
import com.example.chat.repository.FriendRequestRepository;
import com.example.chat.repository.UserRepository;
import com.example.chat.service.MessageService;
import com.example.chat.service.SecurityService;
import com.example.chat.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private FriendRemarkRepository friendRemarkRepository;

    // 存储头像的路径
    private static final String USER_PHOTO_DIR = "E:/Chat_informations/userphoto/";

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @PostMapping("/verify-version")
    public ResponseEntity<Map<String, Object>> verifyVersion(@RequestBody Map<String, String> payload) {
        String clientVersion = payload.get("version");
        // 从配置文件中获取服务器版本号
        String serverVersion = appConfig.getVersion();
        Map<String, Object> response = new HashMap<>();
        if (serverVersion.equals(clientVersion)) {
            response.put("success", true);
            response.put("message", "版本验证通过");
        } else {
            response.put("success", false);
            response.put("message", "版本不匹配，请更新应用");
            response.put("serverVersion", serverVersion);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 注册用户
     */
    @PostMapping(value = "/register", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> registerUser(@RequestBody User user) {
        Map<String, Object> response = new HashMap<>();

        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            response.put("success", false);
            response.put("message", "密码不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        String result = userService.registerUser(user);
        if ("注册成功".equals(result)) {
            // 获取注册用户对象
            User savedUser = userRepository.findByUsername(user.getUsername());
            if (savedUser != null) {
                try {
                    // 设置头像路径
                    String defaultPhotoPath = "E:/Chat_informations/userphoto/default/ic_user.jpg";
                    String userPhotoPath = USER_PHOTO_DIR + savedUser.getId() + ".jpg";

                    // 确保用户头像目录存在
                    File directory = new File(USER_PHOTO_DIR);
                    if (!directory.exists()) {
                        directory.mkdirs();
                    }

                    // 复制默认头像到用户头像路径
                    Path sourcePath = Paths.get(defaultPhotoPath);
                    Path destinationPath = Paths.get(userPhotoPath);
                    Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);

                    // 更新数据库中的 userPhoto 字段
                    savedUser.setUserPhoto(savedUser.getId() + ".jpg");
                    userRepository.save(savedUser);
                } catch (IOException e) {
                    logger.error("保存默认头像失败", e);
                    response.put("success", true);
                    response.put("message", "注册成功，但保存默认头像失败");
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                }
            }
            response.put("success", true);
            response.put("message", "注册成功");
            return ResponseEntity.ok(response);
        }

        response.put("success", false);
        response.put("message", result);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 用户登录
     */
    @PostMapping(value = "/login", consumes = "application/json")
    public ResponseEntity<String> loginUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        if (username == null || password == null) {
            return ResponseEntity.badRequest().body("用户名或密码不能为空");
        }
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("无此用户"); // 用户不存在
        }
        String result = userService.loginUser(username, password);
        if ("登录成功".equals(result)) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("账号或密码错误"); // 密码错误
    }

    /**
     * 检查用户名是否存在
     */
    @PostMapping("/check-username")
    public ResponseEntity<Boolean> checkUsername(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        if (username == null || username.isEmpty()) {
            return ResponseEntity.badRequest().body(false);
        }
        boolean exists = userService.isUsernameExists(username);
        return ResponseEntity.ok(exists);
    }

    /**
     * 更新昵称
     */
    @PostMapping("/update-nickname")
    public ResponseEntity<String> updateNickname(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String nickname = payload.get("nickname");
        if (username == null || nickname == null) {
            return ResponseEntity.badRequest().body("缺少必要参数");
        }
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return ResponseEntity.badRequest().body("用户不存在");
        }
        user.setNickname(nickname);
        userRepository.save(user);
        return ResponseEntity.ok("昵称更新成功");
    }

    /**
     * 获取用户详情
     */
    @PostMapping("/details")
    public ResponseEntity<User> getUserDetails(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        if (username == null || username.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(user);
    }

    /**
     * 更新用户名
     */
    @PostMapping("/update-username")
    public ResponseEntity<String> updateUsername(@RequestBody Map<String, String> payload) {
        String currentUsername = payload.get("currentUsername");
        String newUsername = payload.get("newUsername");
        if (currentUsername == null || newUsername == null) {
            return ResponseEntity.badRequest().body("缺少必要参数");
        }
        if (userRepository.existsByUsername(newUsername)) {
            return ResponseEntity.badRequest().body("该账号已被使用");
        }
        User user = userRepository.findByUsername(currentUsername);
        if (user == null) {
            return ResponseEntity.badRequest().body("用户不存在");
        }
        user.setUsername(newUsername);
        userRepository.save(user);
        return ResponseEntity.ok("用户名更新成功");
    }

    /**
     * 更新用户头像
     */
    @PostMapping("/update-photo")
    public ResponseEntity<String> updatePhoto(
            @RequestParam("username") String username,
            @RequestParam("photo") MultipartFile photo) {
        if (username == null || username.isEmpty() || photo == null || photo.isEmpty()) {
            return ResponseEntity.badRequest().body("用户名或图片不能为空");
        }
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return ResponseEntity.badRequest().body("用户不存在");
        }
        try {
            // 确保存放头像的目录存在
            File directory = new File(USER_PHOTO_DIR);
            if (!directory.exists()) {
                directory.mkdirs(); // 创建目录
            }
            // 使用用户的唯一ID命名图片，并将图片存储到本地
            String photoName = user.getId() + ".jpg";
            Path photoPath = Paths.get(USER_PHOTO_DIR + photoName);
            Files.write(photoPath, photo.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            // 更新数据库中的 userPhoto 字段
            user.setUserPhoto(photoName);
            userRepository.save(user);
            // 返回头像文件名
            return ResponseEntity.ok(photoName);
        } catch (IOException e) {
            logger.error("保存图片失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("保存图片失败");
        }
    }

    /**
     * 获取用户头像
     */
    @GetMapping("/user-photo/{photoName}")
    public ResponseEntity<byte[]> getUserPhoto(@PathVariable String photoName) {
        try {
            // 指定存储用户照片的路径
            Path photoPath = Paths.get(USER_PHOTO_DIR + photoName);
            // 检查文件是否存在
            if (!Files.exists(photoPath)) {
                return ResponseEntity.notFound().build();
            }
            // 读取文件为字节数组
            byte[] photoBytes = Files.readAllBytes(photoPath);
            // 根据文件类型设置Content-Type
            String contentType = Files.probeContentType(photoPath);
            return ResponseEntity.ok()
                    .header("Content-Type", contentType != null ? contentType : "application/octet-stream")
                    .body(photoBytes);
        } catch (IOException e) {
            logger.error("读取图片失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 更新用户邮箱
     */
    @PostMapping("/update-email")
    public ResponseEntity<String> updateEmail(@RequestBody Map<String, String> payload) {
        String currentEmail = payload.get("currentEmail");
        String newEmail = payload.get("newEmail");
        if (currentEmail == null || newEmail == null) {
            return ResponseEntity.badRequest().body("缺少必要参数");
        }
        // 检查新邮箱是否已被使用
        if (userRepository.existsByEmail(newEmail)) {
            return ResponseEntity.badRequest().body("该邮箱已被注册");
        }
        // 获取当前用户
        User user = userRepository.findByEmail(currentEmail);
        if (user == null) {
            return ResponseEntity.badRequest().body("当前邮箱不存在");
        }
        // 更新邮箱
        user.setEmail(newEmail);
        userRepository.save(user);
        return ResponseEntity.ok("邮箱更新成功");
    }

    /**
     * 验证当前密码是否正确
     */
    @PostMapping("/verify-password")
    public ResponseEntity<Map<String, Object>> verifyPassword(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("isValid", false, "message", "缺少必要参数"));
        }

        User user = userRepository.findByUsername(username);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("isValid", false, "message", "用户不存在"));
        }

        boolean isValid = userService.verifyPassword(username, password);
        if (isValid) {
            return ResponseEntity.ok(Map.of("isValid", true, "message", "密码正确"));
        } else {
            return ResponseEntity.ok(Map.of("isValid", false, "message", "密码不正确"));
        }
    }

    /**
     * 更新密码
     */
    @PostMapping("/update-password")
    public ResponseEntity<Map<String, Object>> updatePassword(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String newPassword = payload.get("newPassword");

        if (username == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "缺少必要参数"));
        }

        User user = userRepository.findByUsername(username);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "用户不存在"));
        }

        userService.updatePassword(username, newPassword);
        return ResponseEntity.ok(Map.of("success", true, "message", "密码更新成功"));
    }

    /**
     * 获取用户的密保问题（包括答案）
     */
    @GetMapping("/security-questions")
    public ResponseEntity<List<Map<String, String>>> getSecurityQuestions(@RequestParam String username) {
        try {
            List<SecurityQuestion> questions = securityService.getQuestionsByUser(username);
            List<Map<String, String>> response = questions.stream()
                    .map(q -> Map.of("question", q.getQuestion(), "answer", q.getAnswer())) // 返回实际答案
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("获取密保问题失败", e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * 添加密保问题
     */
    @PostMapping("/security-questions")
    public ResponseEntity<String> addSecurityQuestions(@RequestBody Map<String, Object> payload) {
        String username = (String) payload.get("username");
        List<Map<String, String>> questions = (List<Map<String, String>>) payload.get("questions");

        try {
            List<SecurityQuestion> securityQuestions = questions.stream()
                    .map(q -> {
                        SecurityQuestion sq = new SecurityQuestion();
                        sq.setQuestion(q.get("question"));
                        sq.setAnswer(q.get("answer"));
                        return sq;
                    })
                    .collect(Collectors.toList());
            securityService.addQuestions(username, securityQuestions);
            return ResponseEntity.ok("密保问题添加成功");
        } catch (IllegalArgumentException e) {
            logger.error("添加密保问题失败", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 删除密保问题
     */
    @DeleteMapping("/security-questions")
    public ResponseEntity<String> deleteSecurityQuestion(@RequestParam String username, @RequestParam String question) {
        try {
            securityService.deleteQuestion(username, question);
            return ResponseEntity.ok("密保问题删除成功");
        } catch (IllegalArgumentException e) {
            logger.error("删除密保问题失败", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 更新密保问题
     */
    @PutMapping("/security-questions")
    public ResponseEntity<String> updateSecurityQuestion(@RequestBody Map<String, Object> payload) {
        String username = (String) payload.get("username");
        String oldQuestion = (String) payload.get("oldQuestion");
        Map<String, String> newQuestionData = (Map<String, String>) payload.get("newQuestion");

        try {
            SecurityQuestion newQuestion = new SecurityQuestion();
            newQuestion.setQuestion(newQuestionData.get("question"));
            newQuestion.setAnswer(newQuestionData.get("answer"));
            securityService.updateQuestion(username, oldQuestion, newQuestion);
            return ResponseEntity.ok("密保问题更新成功");
        } catch (IllegalArgumentException e) {
            logger.error("更新密保问题失败", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 获取用户的密保问题（不包括答案）
     */
    @GetMapping("/get-security-questions")
    public ResponseEntity<List<Map<String, String>>> fetchSecurityQuestions(@RequestParam String username) {
        try {
            List<SecurityQuestion> questions = securityService.getQuestionsByUser(username);
            List<Map<String, String>> response = questions.stream()
                    .map(q -> Map.of("question", q.getQuestion()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("获取密保问题失败", e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * 验证密保答案
     */
    @PostMapping("/validate-security-answers")
    public ResponseEntity<Map<String, Object>> validateSecurityAnswers(@RequestBody SecurityAnswerRequest request) {
        String username = request.getUsername();
        List<SecurityAnswerDTO> answers = request.getAnswers();

        Map<String, Object> response = new HashMap<>();

        if (username == null || answers == null || answers.size() != 3) {
            response.put("success", false);
            response.put("message", "缺少必要参数或答案数量不正确");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        try {
            boolean isValid = securityService.verifyAnswers(username, answers);
            if (isValid) {
                response.put("success", true);
                response.put("message", "验证成功");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "答案不正确");
                return ResponseEntity.ok(response);
            }
        } catch (IllegalArgumentException e) {
            logger.error("验证密保答案失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            logger.error("服务器内部错误", e);
            response.put("success", false);
            response.put("message", "服务器内部错误");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 添加好友申请接口
     */
    @PostMapping("/friends/add-request")
    public ResponseEntity<Map<String, String>> addFriendRequest(@RequestBody Map<String, String> payload) {
        String fromUsername = payload.get("fromUsername");
        String toUsername = payload.get("toUsername");

        Map<String, String> response = new HashMap<>();

        if (fromUsername == null || toUsername == null) {
            response.put("success", "false");
            response.put("message", "缺少必要参数");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        User fromUser = userRepository.findByUsername(fromUsername);
        User toUser = userRepository.findByUsername(toUsername);

        if (fromUser == null || toUser == null) {
            response.put("success", "false");
            response.put("message", "用户不存在");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (fromUser.getId().equals(toUser.getId())) {
            response.put("success", "false");
            response.put("message", "无法添加自己为好友");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // 检查是否已经是好友
        if (friendRepository.existsByUserAndFriend(fromUser, toUser)) {
            response.put("success", "false");
            response.put("message", "已经是好友");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // 查询是否已经存在好友请求
        Optional<FriendRequest> existingRequestOpt = friendRequestRepository.findByFromUserAndToUser(fromUser, toUser);

        if (existingRequestOpt.isPresent()) {
            FriendRequest existingRequest = existingRequestOpt.get();
            if (existingRequest.getStatus() == FriendRequestStatus.PENDING) {
                response.put("success", "false");
                response.put("message", "好友请求已发送");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            } else if (existingRequest.getStatus() == FriendRequestStatus.DECLINED) {
                // 更新状态为 PENDING
                existingRequest.setStatus(FriendRequestStatus.PENDING);
                friendRequestRepository.save(existingRequest);
                response.put("success", "true");
                response.put("message", "好友请求已重新发送");
                return ResponseEntity.ok(response);
            } else if (existingRequest.getStatus() == FriendRequestStatus.ACCEPTED) {
                response.put("success", "false");
                response.put("message", "已经是好友");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        }

        // 检查是否有从对方发出的好友请求，如果存在，则自动接受
        Optional<FriendRequest> reverseRequestOpt = friendRequestRepository.findByFromUserAndToUser(toUser, fromUser);
        if (reverseRequestOpt.isPresent()) {
            FriendRequest reverseRequest = reverseRequestOpt.get();
            if (reverseRequest.getStatus() == FriendRequestStatus.PENDING) {
                // 自动接受对方的好友请求
                reverseRequest.setStatus(FriendRequestStatus.ACCEPTED);
                friendRequestRepository.save(reverseRequest);
                // 创建双向好友关系
                Friend friend1 = new Friend();
                friend1.setUser(fromUser);
                friend1.setFriend(toUser);

                Friend friend2 = new Friend();
                friend2.setUser(toUser);
                friend2.setFriend(fromUser);

                friendRepository.save(friend1);
                friendRepository.save(friend2);

                response.put("success", "true");
                response.put("message", "好友请求已自动接受，您们已成为好友");
                return ResponseEntity.ok(response);
            }
        }

        // 创建新的好友请求
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setFromUser(fromUser);
        friendRequest.setToUser(toUser);
        friendRequest.setStatus(FriendRequestStatus.PENDING);
        friendRequestRepository.save(friendRequest);

        response.put("success", "true");
        response.put("message", "好友请求已发送");
        return ResponseEntity.ok(response);
    }

    /**
     * 获取收到的好友申请
     */
    @GetMapping("/friends/requests")
    public ResponseEntity<List<FriendRequestDTO>> getFriendRequests(@RequestParam String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        List<FriendRequest> requests = friendRequestRepository.findByFromUserOrToUser(user, user);
        // 将实体转换为 DTO
        List<FriendRequestDTO> dtoList = requests.stream().map(request -> {
            FriendRequestDTO dto = new FriendRequestDTO();
            dto.setId(request.getId());
            dto.setStatus(request.getStatus().name());
            // 判断是发送的请求还是收到的请求
            if (request.getFromUser().equals(user)) {
                dto.setType("SENT"); // 自己发送的请求
                dto.setUser(convertToDTO(request.getToUser()));
            } else {
                dto.setType("RECEIVED"); // 收到的请求
                dto.setUser(convertToDTO(request.getFromUser()));
            }
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }

    /**
     * 转换 User 实体为 UserDTO
     */
    private UserDTO convertToDTO(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setNickname(user.getNickname());
        userDTO.setUserPhoto(user.getUserPhoto());
        return userDTO;
    }

    /**
     * 同意好友申请接口
     */
    @PostMapping("/friends/accept")
    public ResponseEntity<Map<String, String>> acceptFriendRequest(@RequestBody Map<String, Long> payload) {
        Long requestId = payload.get("requestId");
        FriendRequest request = friendRequestRepository.findById(requestId).orElse(null);
        if (request == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "好友申请不存在");
            return ResponseEntity.badRequest().body(response);
        }

        // 更新请求状态
        request.setStatus(FriendRequestStatus.ACCEPTED);
        friendRequestRepository.save(request);

        // 创建双向好友关系
        Friend friend1 = new Friend();
        friend1.setUser(request.getFromUser());
        friend1.setFriend(request.getToUser());

        Friend friend2 = new Friend();
        friend2.setUser(request.getToUser());
        friend2.setFriend(request.getFromUser());

        friendRepository.save(friend1);
        friendRepository.save(friend2);

        Map<String, String> response = new HashMap<>();
        response.put("message", "好友申请已同意");
        return ResponseEntity.ok(response);
    }

    /**
     * 拒绝好友申请接口
     */
    @PostMapping("/friends/decline")
    public ResponseEntity<Map<String, String>> declineFriendRequest(@RequestBody Map<String, Long> payload) {
        Long requestId = payload.get("requestId");
        FriendRequest request = friendRequestRepository.findById(requestId).orElse(null);
        if (request == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "好友申请不存在");
            return ResponseEntity.badRequest().body(response);
        }

        // 更新请求状态
        request.setStatus(FriendRequestStatus.DECLINED);
        friendRequestRepository.save(request);

        Map<String, String> response = new HashMap<>();
        response.put("message", "好友申请已拒绝");
        return ResponseEntity.ok(response);
    }

    /**
     * 获取已发送的好友申请
     */
    @GetMapping("/friends/sentRequests")
    public ResponseEntity<List<FriendRequestDTO>> getSentFriendRequests(@RequestParam String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        List<FriendRequest> requests = friendRequestRepository.findByFromUser(user);
        // 将实体转换为 DTO
        List<FriendRequestDTO> dtoList = requests.stream().map(request -> {
            FriendRequestDTO dto = new FriendRequestDTO();
            dto.setId(request.getId());
            dto.setStatus(request.getStatus().name());
            UserDTO toUserDTO = convertToDTO(request.getToUser());
            dto.setType("SENT");
            dto.setUser(toUserDTO);
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }

    /**
     * 标记已发送的好友申请为已查看
     */
    @PostMapping("/friends/markSentRequestsAsViewed")
    public ResponseEntity<Void> markSentRequestsAsViewed(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        List<FriendRequest> requests = friendRequestRepository.findByFromUser(user);
        for (FriendRequest request : requests) {
            request.setIsViewed(true);
        }
        friendRequestRepository.saveAll(requests);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取好友列表
     */
    @GetMapping("/friends/list")
    public ResponseEntity<List<UserDTO>> getFriendsList(@RequestParam String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        List<Friend> friends = friendRepository.findByUser(user);
        List<UserDTO> friendUsers = friends.stream()
                .map(friend -> {
                    User friendUser = friend.getFriend();
                    UserDTO userDTO = new UserDTO();
                    userDTO.setId(friendUser.getId());
                    userDTO.setUsername(friendUser.getUsername());
                    userDTO.setNickname(friendUser.getNickname());
                    userDTO.setUserPhoto(friendUser.getUserPhoto());
                    return userDTO;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(friendUsers);
    }

    /**
     * 删除好友
     */
    @DeleteMapping("/delete_friend")
    public ResponseEntity<Map<String, String>> deleteFriend(@RequestParam("username") String username,
                                                            @RequestParam("friendUsername") String friendUsername) {
        // 添加日志
        logger.info("Attempting to delete friend. Username: {}, FriendUsername: {}", username, friendUsername);

        User user = userRepository.findByUsername(username);
        User friend = userRepository.findByUsername(friendUsername);

        if (user == null || friend == null) {
            logger.warn("User or Friend not found. Username: {}, FriendUsername: {}", username, friendUsername);
            Map<String, String> response = new HashMap<>();
            response.put("error", "用户不存在");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // 检查是否是好友
        Optional<Friend> existingFriendOpt1 = friendRepository.findByUserAndFriend(user, friend);
        Optional<Friend> existingFriendOpt2 = friendRepository.findByUserAndFriend(friend, user);
        if (!existingFriendOpt1.isPresent() || !existingFriendOpt2.isPresent()) {
            logger.warn("Users are not friends. Username: {}, FriendUsername: {}", username, friendUsername);
            Map<String, String> response = new HashMap<>();
            response.put("error", "不是好友关系");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // 删除双向好友关系
        friendRepository.delete(existingFriendOpt1.get());
        friendRepository.delete(existingFriendOpt2.get());

        // 删除相关的好友请求记录
        Optional<FriendRequest> existingRequestOpt = friendRequestRepository.findByFromUserAndToUser(user, friend);
        existingRequestOpt.ifPresent(friendRequestRepository::delete);

        Optional<FriendRequest> reverseRequestOpt = friendRequestRepository.findByFromUserAndToUser(friend, user);
        reverseRequestOpt.ifPresent(friendRequestRepository::delete);

        logger.info("Successfully deleted friend. Username: {}, FriendUsername: {}", username, friendUsername);

        // 返回成功的 JSON 响应
        Map<String, String> response = new HashMap<>();
        response.put("message", "好友已删除");
        return ResponseEntity.ok(response);
    }


    /**
     * 获取好友列表及其最新消息
     */
    @GetMapping("/friends/withLatestMessage")
    public ResponseEntity<List<Map<String, Object>>> getFriendsWithLatestMessage(@RequestParam String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        List<Friend> friends = friendRepository.findByUser(user);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Friend friend : friends) {
            User friendUser = friend.getFriend();
            Map<String, Object> map = new HashMap<>();
            map.put("friend", convertToDTO(friendUser));
            Message latestMessage = messageService.getLatestMessage(username, friendUser.getUsername());
            if (latestMessage != null) {
                map.put("latestMessage", latestMessage.getContent());
                map.put("timestamp", latestMessage.getTimestamp());
            } else {
                map.put("latestMessage", "");
                map.put("timestamp", null);
            }
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 获取好友详细信息
     */
    @GetMapping("/friend/info")
    public ResponseEntity<Map<String, Object>> getFriendInfo(
            @RequestParam String username,
            @RequestParam String friendUsername) {
        User user = userRepository.findByUsername(username);
        User friend = userRepository.findByUsername(friendUsername);

        if (user == null || friend == null) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", friend.getId());
        response.put("username", friend.getUsername());
        response.put("nickname", friend.getNickname());
        response.put("userPhoto", friend.getUserPhoto());

        // 获取备注
        Optional<FriendRemark> remarkOpt = friendRemarkRepository.findByUserAndFriend(user, friend);
        response.put("remark", remarkOpt.map(FriendRemark::getRemark).orElse(""));

        return ResponseEntity.ok(response);
    }

    /**
     * 更新好友备注
     */
    @PostMapping("/friend/remark")
    public ResponseEntity<String> updateFriendRemark(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String friendUsername = payload.get("friendUsername");
        String remark = payload.get("remark");

        User user = userRepository.findByUsername(username);
        User friend = userRepository.findByUsername(friendUsername);

        if (user == null || friend == null) {
            return ResponseEntity.badRequest().body("用户不存在");
        }

        FriendRemark friendRemark = friendRemarkRepository
                .findByUserAndFriend(user, friend)
                .orElse(new FriendRemark());

        friendRemark.setUser(user);
        friendRemark.setFriend(friend);
        friendRemark.setRemark(remark);

        friendRemarkRepository.save(friendRemark);
        return ResponseEntity.ok("标签更新成功");
    }

}
