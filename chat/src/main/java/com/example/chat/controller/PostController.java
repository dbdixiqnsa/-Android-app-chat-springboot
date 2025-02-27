package com.example.chat.controller;

import com.example.chat.dto.PostDTO;
import com.example.chat.dto.UserDTO;
import com.example.chat.model.*;
import com.example.chat.repository.FriendRepository;
import com.example.chat.repository.PostLikeRepository;
import com.example.chat.repository.PostRepository;
import com.example.chat.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendRepository friendRepository;

    // 存储动态图片的路径
    private static final String POST_IMAGE_DIR = "E:/Chat_informations/post_images/";

    private static final Logger logger = LoggerFactory.getLogger(PostController.class);

    /**
     * 获取时间线动态（包括自己的和好友的）
     */
    @Transactional
    @GetMapping("/timeline")
    public ResponseEntity<List<PostDTO>> getTimelinePosts(@RequestParam("username") String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            logger.warn("User {} not found.", username);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        // 获取好友列表
        List<Friend> friendRelations = friendRepository.findByUser(user);
        List<User> friends = friendRelations.stream()
                .map(Friend::getFriend)
                .collect(Collectors.toList());

        List<User> users = new ArrayList<>(friends);
        users.add(user); // 添加当前用户

        logger.info("Fetching posts for user {} and their {} friends.", username, friends.size());

        // 获取这些用户的所有动态
        List<Post> posts = postRepository.findByUserIn(users);

        logger.info("Found {} posts for timeline.", posts.size());

        // 将动态转换为DTO并设置是否被当前用户点赞
        List<PostDTO> postDTOs = posts.stream()
                .map(post -> {
                    PostDTO dto = convertToDTO(post);
                    boolean isLiked = postLikeRepository.existsByPostIdAndUserId(post.getId(), user.getId());
                    dto.setLikedByCurrentUser(isLiked);
                    logger.debug("Post ID: {}, likedByCurrentUser: {}", post.getId(), isLiked);
                    return dto;
                })
                .sorted(Comparator.comparing(PostDTO::getTimestamp).reversed()) // 按时间降序排序
                .collect(Collectors.toList());

        return ResponseEntity.ok(postDTOs);
    }

    /**
     * 获取所有动态
     */
    @GetMapping("/all")
    public ResponseEntity<List<PostDTO>> getAllPosts(@RequestParam("username") String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        List<Post> posts = postRepository.findAll();
        List<PostDTO> postDTOs = posts.stream()
                .map(post -> {
                    PostDTO dto = convertToDTO(post);
                    boolean isLiked = postLikeRepository.existsByPostIdAndUserId(post.getId(), user.getId());
                    dto.setLikedByCurrentUser(isLiked);
                    logger.debug("Post ID: {}, likedByCurrentUser: {}", post.getId(), isLiked);
                    return dto;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(postDTOs);
    }


    /**
     * 删除动态
     *
     * @param postId  动态的ID
     * @param username 当前请求的用户名
     * @return 响应实体
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<Map<String, String>> deletePost(
            @PathVariable Long postId,
            @RequestParam("username") String username
    ) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            Map<String, String> response = new HashMap<>();
            response.put("success", "false");
            response.put("message", "用户不存在");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        Optional<Post> optionalPost = postRepository.findById(postId);
        if (!optionalPost.isPresent()) {
            Map<String, String> response = new HashMap<>();
            response.put("success", "false");
            response.put("message", "动态不存在");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        Post post = optionalPost.get();

        // 验证当前用户是否是动态的发布者
        if (!post.getUser().getId().equals(user.getId())) {
            Map<String, String> response = new HashMap<>();
            response.put("success", "false");
            response.put("message", "无权限删除此动态");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        try {
            // 删除关联的图片文件
            if (post.getPostImages() != null) {
                for (PostImage postImage : post.getPostImages()) {
                    Path imagePath = Paths.get(POST_IMAGE_DIR + postImage.getImageUrl());
                    Files.deleteIfExists(imagePath);
                }
            }

            // 删除动态及其关联的点赞和图片记录
            postRepository.delete(post);

            Map<String, String> response = new HashMap<>();
            response.put("success", "true");
            response.put("message", "动态删除成功");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            e.printStackTrace();
            Map<String, String> response = new HashMap<>();
            response.put("success", "false");
            response.put("message", "删除动态时发生错误");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 创建新的动态
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostDTO> createPost(
            @RequestPart("username") String username,
            @RequestPart("title") String title,
            @RequestPart("content") String content,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        Post post = new Post();
        post.setUser(user);
        post.setTitle(title);
        post.setContent(content);
        post.setTimestamp(LocalDateTime.now());
        post.setLikeCount(0);

        // 确保 postImages 列表被初始化
        List<PostImage> postImages = new ArrayList<>();

        // 处理图片上传
        if (images != null && !images.isEmpty()) {
            for (MultipartFile image : images) {
                if (image.isEmpty()) continue;
                try {
                    // 确保目录存在
                    Path dirPath = Paths.get(POST_IMAGE_DIR);
                    if (!Files.exists(dirPath)) {
                        Files.createDirectories(dirPath);
                    }
                    String originalFilename = image.getOriginalFilename();
                    String uniqueFilename = UUID.randomUUID().toString() + "_" + originalFilename;
                    Path filePath = dirPath.resolve(uniqueFilename);
                    Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                    PostImage postImage = new PostImage();
                    postImage.setPost(post);
                    postImage.setImageUrl(uniqueFilename);
                    postImages.add(postImage);
                } catch (IOException e) {
                    e.printStackTrace();
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
                }
            }
        }

        post.setPostImages(postImages); // 即使没有图片，也设置为空列表

        Post savedPost = postRepository.save(post);
        PostDTO postDTO = convertToDTO(savedPost);
        return ResponseEntity.ok(postDTO);
    }

    // 点赞动态
    @PostMapping("/{postId}/like")
    public ResponseEntity<Map<String, String>> toggleLikePost(
            @PathVariable Long postId,
            @RequestParam("username") String username
    ) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "用户不存在");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        Optional<Post> optionalPost = postRepository.findById(postId);
        if (!optionalPost.isPresent()) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "动态不存在");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        Post post = optionalPost.get();

        // 检查用户是否已经点赞
        Optional<PostLike> optionalLike = postLikeRepository.findByPostIdAndUserId(postId, user.getId());

        if (optionalLike.isPresent()) {
            // 取消点赞
            postLikeRepository.delete(optionalLike.get());
            post.setLikeCount(post.getLikeCount() - 1);
            postRepository.save(post);
            Map<String, String> response = new HashMap<>();
            response.put("message", "取消点赞");
            return ResponseEntity.ok(response);
        } else {
            // 点赞
            PostLike postLike = new PostLike();
            postLike.setPost(post);
            postLike.setUser(user);
            postLike.setTimestamp(LocalDateTime.now());

            postLikeRepository.save(postLike);

            // 更新点赞数
            post.setLikeCount(post.getLikeCount() + 1);
            postRepository.save(post);
            Map<String, String> response = new HashMap<>();
            response.put("message", "点赞成功");
            return ResponseEntity.ok(response);
        }
    }

    // 辅助方法：将Post转换为PostDTO
    private PostDTO convertToDTO(Post post) {
        PostDTO dto = new PostDTO();
        dto.setId(post.getId());
        dto.setUser(convertToDTO(post.getUser()));
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setTimestamp(post.getTimestamp().toString()); // 设置为 String 类型
        dto.setLikeCount(post.getLikeCount());

        // 设置图片 URLs
        List<String> imageUrls = post.getPostImages() != null ?
                post.getPostImages().stream()
                        .map(postImage -> "http://s90020407x.goho.co/api/posts/images/" + postImage.getImageUrl())
                        .collect(Collectors.toList()) : new ArrayList<>();
        dto.setImages(imageUrls);

        // 不在此处设置 likedByCurrentUser

        return dto;
    }



    // 辅助方法：将User转换为UserDTO
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setUserPhoto(user.getUserPhoto());
        return dto;
    }

    // 获取帖子图片
    @GetMapping("/images/{imageName}")
    public ResponseEntity<byte[]> getPostImage(@PathVariable String imageName) {
        try {
            Path imagePath = Paths.get(POST_IMAGE_DIR + imageName);
            if (!Files.exists(imagePath)) {
                return ResponseEntity.notFound().build();
            }
            byte[] imageBytes = Files.readAllBytes(imagePath);
            String contentType = Files.probeContentType(imagePath);
            return ResponseEntity.ok()
                    .header("Content-Type", contentType != null ? contentType : "application/octet-stream")
                    .body(imageBytes);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 获取好友的动态
    @GetMapping("/get_friends_posts")
    public ResponseEntity<List<PostDTO>> getFriendsPosts(@RequestParam("username") String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        // 使用 FriendRepository 获取好友列表
        List<Friend> friendRelations = friendRepository.findByUser(user);
        List<User> friends = friendRelations.stream()
                .map(Friend::getFriend)
                .collect(Collectors.toList());

        if (friends.isEmpty()) {
            // 用户没有好友，返回空列表
            return ResponseEntity.ok(new ArrayList<>());
        }

        List<Post> friendPosts = postRepository.findByUserIn(friends);

        List<PostDTO> postDTOs = friendPosts.stream()
                .map(post -> {
                    PostDTO dto = convertToDTO(post);
                    boolean isLiked = postLikeRepository.existsByPostIdAndUserId(post.getId(), user.getId());
                    dto.setLikedByCurrentUser(isLiked);
                    logger.debug("Post ID: {}, likedByCurrentUser: {}", post.getId(), isLiked);
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(postDTOs);
    }

}
