package com.playground.springredis.lock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Component
public class RedisLockRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 분산 락 획득
     * @param key 락 키
     * @param value 락 값 (고유 식별자) 해제시 필요
     * @param durationMillis 락 유지 시간 (밀리초)
     * @return
     */
    public Boolean lock(String key, String value, long durationMillis) {
        // if setIfAbsent는 SET key value NX 와 동일
        // NX 옵션은 key가 존재하지 않을 때에만 값을 설정
        return redisTemplate.opsForValue()
                .setIfAbsent(key, value, Duration.ofMillis(durationMillis));

    }

    /**
     * 분산 락 해제
     * @param key 락 키
     * @param value 락 값 (고유 식별자) 해제시 필요
     */
    public boolean unlock(String key, String value) {
        // Lua 스크립트를 사용하여 "값 비교"와 "삭제"를 원자적으로 처리
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "    return redis.call('del', KEYS[1]) " +
                    "else " +
                    "    return 0 " +
                    "end";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);


        Long result = redisTemplate.execute(
                redisScript, // 실행할 Lua 스크립트
                (List<String>) Collections.singletonList(key), // KEYS 배열에 전달할 키 목록
                value // ARGV 배열에 전달할
        );
        return result != null && result == 1L;
    }
}
