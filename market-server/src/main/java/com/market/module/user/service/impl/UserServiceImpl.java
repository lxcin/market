package com.market.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.market.common.exception.BusinessException;
import com.market.common.metrics.BusinessMetrics;
import com.market.common.response.Result;
import com.market.common.service.VerificationCodeService;
import com.market.common.util.JwtUtil;
import com.market.module.user.entity.User;
import com.market.module.user.mapper.UserMapper;
import com.market.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int MAX_LOGIN_FAILURES = 5;
    private static final Duration LOGIN_LOCK_TTL = Duration.ofMinutes(15);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final VerificationCodeService verificationCodeService;
    private final StringRedisTemplate stringRedisTemplate;
    private final BusinessMetrics businessMetrics;

    @Override
    public Result<Map<String, Object>> register(String username, String password, String email, String phone) {
        boolean exists = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0;
        if (exists) {
            throw new BusinessException("用户名已存在");
        }

        if (StringUtils.hasText(phone)) {
            boolean phoneExists = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getPhone, phone)) > 0;
            if (phoneExists) {
                throw new BusinessException("该手机号已被注册");
            }
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setPhone(phone);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("role", user.getRole());

        return Result.success(data);
    }

    @Override
    public Result<Map<String, Object>> login(String username, String password,
                                             String captchaId, String captchaCode) {
        if (!verificationCodeService.verifyCaptcha(captchaId, captchaCode)) {
            throw new BusinessException("验证码错误或已失效");
        }

        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey("auth:login:lock:" + username))) {
            throw new BusinessException("账号已锁定，请 15 分钟后再试");
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null && StringUtils.hasText(username)) {
            user = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getPhone, username));
        }
        if (user == null) {
            recordLoginFailure(username);
            throw new BusinessException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            recordLoginFailure(username);
            throw new BusinessException("用户名或密码错误");
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        stringRedisTemplate.delete("auth:login:fail:" + username);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("role", user.getRole());

        return Result.success(data);
    }

    @Override
    public Map<String, Object> sendResetCode(String phone) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) {
            throw new BusinessException("该手机号未注册");
        }

        String code = verificationCodeService.sendSmsCode(phone);

        Map<String, Object> data = new HashMap<>();
        data.put("mock", verificationCodeService.isSmsMock());
        if (verificationCodeService.isSmsMock()) {
            data.put("code", code);
        }
        return data;
    }

    @Override
    public void resetPassword(String phone, String code, String newPassword) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) {
            throw new BusinessException("该手机号未注册");
        }

        if (!verificationCodeService.verifySmsCode(phone, code)) {
            throw new BusinessException("验证码错误或已失效");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    private void recordLoginFailure(String username) {
        businessMetrics.loginFailure();
        String failKey = "auth:login:fail:" + username;
        Long fails = stringRedisTemplate.opsForValue().increment(failKey);
        if (fails != null && fails == 1L) {
            stringRedisTemplate.expire(failKey, LOGIN_LOCK_TTL);
        }
        if (fails != null && fails >= MAX_LOGIN_FAILURES) {
            stringRedisTemplate.opsForValue().set("auth:login:lock:" + username, "1", LOGIN_LOCK_TTL);
            stringRedisTemplate.delete(failKey);
        }
    }
}
