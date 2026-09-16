package com.market.module.user.service;

import com.market.common.response.Result;
import com.market.module.user.entity.User;

import java.util.Map;

public interface UserService {

    Result<Map<String, Object>> register(String username, String password, String email, String phone);

    Result<Map<String, Object>> login(String username, String password, String captchaId, String captchaCode);

    Map<String, Object> sendResetCode(String phone);

    void resetPassword(String phone, String code, String newPassword);

    User getById(Long id);
}
