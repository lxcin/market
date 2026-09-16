package com.market.module.user.controller;

import com.market.common.response.Result;
import com.market.common.security.TokenBlacklistService;
import com.market.common.service.VerificationCodeService;
import com.market.common.util.JwtUtil;
import com.market.common.util.MaskUtil;
import com.market.common.util.SecurityUtil;
import com.market.module.user.entity.User;
import com.market.module.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final VerificationCodeService verificationCodeService;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @GetMapping("/captcha")
    public Result<Map<String, Object>> captcha() {
        VerificationCodeService.CaptchaResult result = verificationCodeService.createCaptcha();
        Map<String, Object> data = new HashMap<>();
        data.put("captchaId", result.captchaId());
        data.put("image", result.image());
        return Result.success(data);
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request.getUsername(), request.getPassword(),
                request.getEmail(), request.getPhone());
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request.getUsername(), request.getPassword(),
                request.getCaptchaId(), request.getCaptchaCode());
    }

    @PostMapping("/sms/send")
    public Result<Map<String, Object>> sendSmsCode(@Valid @RequestBody SmsSendRequest request) {
        return Result.success(userService.sendResetCode(request.getPhone()));
    }

    @PostMapping("/password/reset")
    public Result<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        userService.resetPassword(request.getPhone(), request.getCode(), request.getNewPassword());
        return Result.success();
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            try {
                String jti = jwtUtil.getJti(token);
                long remaining = jwtUtil.getRemainingMillis(token);
                tokenBlacklistService.blacklist(jti, remaining);
            } catch (Exception ignore) {
                // token 非法则无需拉黑
            }
        }
        return Result.success();
    }

    @GetMapping("/info")
    public Result<User> info() {
        Long userId = SecurityUtil.getCurrentUserId();
        User user = userService.getById(userId);
        if (user != null) {
            user.setPassword(null);
            user.setPhone(MaskUtil.maskPhone(user.getPhone()));
            user.setEmail(MaskUtil.maskEmail(user.getEmail()));
        }
        return Result.success(user);
    }
}
