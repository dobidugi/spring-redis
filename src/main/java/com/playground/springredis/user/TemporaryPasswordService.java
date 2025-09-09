package com.playground.springredis.user;

import com.playground.springredis.Util.RandomStringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@RequiredArgsConstructor
@Service
public class TemporaryPasswordService {

    private final RandomStringUtil randomStringUtil;
    private final String TEMPORARY_PASSWORD_KEY_PREFIX = "temporary_password:";
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 임시 비밀번호 발급
     * @param userId 사용자 ID
     * @param durationInSeconds 유효 기간 (초)
     * @return 발급된 임시 비밀번호
     */
    public String issueTemporaryPassword(Long userId, long durationInSeconds) {
        final String temporaryPassword = randomStringUtil.generate(10);
        String key = TEMPORARY_PASSWORD_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, temporaryPassword);
        redisTemplate.expire(key, Duration.ofSeconds(durationInSeconds));
        return temporaryPassword;
    }

    /**
     * 임시 비밀번호 검증
     * @param userId 사용자 ID
     * @param temporaryPassword 임시 비밀번호
     * @return 검증 성공 여부
     */
    public boolean verifyTemporaryPassword(Long userId, String temporaryPassword) {
        String key = TEMPORARY_PASSWORD_KEY_PREFIX + userId;
        String storedPassword = (String) redisTemplate.opsForValue().get(key);
        if(storedPassword != null && storedPassword.equals(temporaryPassword)) {
            return redisTemplate.delete(key); // 검증 성공 시 임시 비밀번호 삭제
        } else {
            return false;
        }
    }


}
