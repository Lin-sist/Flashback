package com.flashback.mapper;

import com.flashback.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface UserMapper {

    User selectByUsername(@Param("username") String username);

    User selectById(@Param("id") Long id);

    User selectByOpenid(@Param("openid") String openid);

    int insert(User user);

    int updateProfileById(
            @Param("id") Long id,
            @Param("nickname") String nickname,
            @Param("email") String email,
            @Param("avatar") String avatar,
            @Param("updatedAt") LocalDateTime updatedAt);

    int updateOpenidById(
            @Param("id") Long id,
            @Param("openid") String openid,
            @Param("updatedAt") LocalDateTime updatedAt);
}
