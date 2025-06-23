package com.example.chat.network;

import com.example.chat.dto.PostDTO;
import com.example.chat.model.FriendRequest;
import com.example.chat.model.User;
import com.example.chat.dto.MessageDTO;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    @POST("users/login")
    Call<String> loginUser(@Body Map<String, String> payload);

    @POST("users/register")
    Call<String> registerUser(@Body Map<String, String> payload);

    @POST("users/friends/add-request")
    Call<Map<String, String>> addFriendRequest(@Body Map<String, String> payload);

    @GET("users/friends/list")
    Call<List<User>> getFriendsList(@Query("username") String username);

    // 获取收到的好友申请列表
    @GET("/api/users/friends/requests")
    Call<List<FriendRequest>> getFriendRequests(@Query("username") String username);

    // 获取发送的好友申请列表
    @GET("users/friends/sentRequests")
    Call<List<FriendRequest>> getSentFriendRequests(@Query("username") String username);

    // 标记收到的好友申请为已查看
    @POST("users/friends/markViewed")
    Call<Void> markRequestsAsViewed(@Body Map<String, String> payload);

    // 标记发送的好友申请为已查看
    @POST("users/friends/markSentRequestsAsViewed")
    Call<Void> markSentRequestsAsViewed(@Body Map<String, String> payload);

    // 同意好友申请
    @POST("users/friends/accept")
    Call<Map<String, String>> acceptFriendRequest(@Body Map<String, Long> payload);

    // 拒绝好友申请
    @POST("users/friends/decline")
    Call<Map<String, String>> declineFriendRequest(@Body Map<String, Long> payload);

    @DELETE("users/delete_friend")
    Call<Map<String, String>> deleteFriend(@Query("username") String username, @Query("friendUsername") String friendUsername);

    // 发送消息
    @POST("messages/send")
    Call<MessageDTO> sendMessage(@Body MessageDTO messageDTO);

    // 获取两个用户之间的消息
    @POST("messages/chat")
    Call<List<MessageDTO>> getMessages(@Body Map<String, String> payload);

    // 获取用户的最近联系人及其最新消息
    @POST("messages/latest")
    Call<List<MessageDTO>> getLatestMessages(@Body Map<String, String> payload);

    // 获取用户详情
    @POST("users/details")
    Call<User> getUserDetails(@Body Map<String, String> payload);

    // 标记消息为已读
    @POST("messages/markAsRead")
    Call<Void> markMessagesAsRead(@Body Map<String, String> payload);

    // 获取未读消息数量
    @GET("messages/unreadCounts")
    Call<List<Map<String, Object>>> getUnreadCounts(@Query("username") String username);

    // 获取好友列表及其最新消息
    @GET("users/friends/withLatestMessage")
    Call<List<Map<String, Object>>> getFriendsWithLatestMessage(@Query("username") String username);

    // 获取所有动态（帖子）
    @GET("posts")
    Call<List<PostDTO>> getAllPosts(@Query("username") String username);

    // 创建新的动态，支持多张图片
    @Multipart
    @POST("posts")
    Call<PostDTO> createPost(
            @Part("username") RequestBody username,
            @Part("title") RequestBody title,
            @Part("content") RequestBody content,
            @Part List<MultipartBody.Part> images
    );

    // 点赞或取消点赞动态
    @POST("posts/{postId}/like")
    @FormUrlEncoded
    Call<Map<String, String>> toggleLikePost(@Path("postId") Long postId, @Field("username") String username);

    // 获取动态图片
    @GET("posts/images/{imageName}")
    Call<ResponseBody> getPostImage(@Path("imageName") String imageName);

    // 删除动态
    @DELETE("posts/{postId}")
    Call<Map<String, String>> deletePost(@Path("postId") Long postId, @Query("username") String username);

    // 获取好友的动态
    @GET("posts/get_friends_posts")
    Call<List<PostDTO>> getFriendsPosts(@Query("username") String username);

    // 获取所有动态（包括自己的和好友的）
    @GET("posts/timeline")
    Call<List<PostDTO>> getTimelinePosts(@Query("username") String username);

    @GET("users/friend/info")
    Call<Map<String, Object>> getFriendInfo(
        @Query("username") String username,
        @Query("friendUsername") String friendUsername
    );

    @POST("users/friend/remark")
    Call<ResponseBody> updateFriendRemark(
        @Body Map<String, String> payload
    );
}
