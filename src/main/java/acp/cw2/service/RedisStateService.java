package acp.cw2.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisStateService {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisStateService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public Integer getVersion(String key) {
        String value = stringRedisTemplate.opsForValue().get(versionKey(key));
        return value == null ? null : Integer.valueOf(value);
    }

    public void setVersion(String key, int version) {
        stringRedisTemplate.opsForValue().set(versionKey(key), String.valueOf(version));
    }

    public void deleteVersion(String key) {
        stringRedisTemplate.delete(versionKey(key));
    }

    public void clearAllTransformVersions() {
        var keys = stringRedisTemplate.keys("transform:version:*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    private String versionKey(String key) {
        return "transform:version:" + key;
    }
}