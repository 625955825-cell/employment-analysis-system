package com.employment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String TEACHER_STATS_PREFIX = "teacher:stats:";
    private static final String ANALYTICS_STATS_PREFIX = "analytics:stats:";
    private static final long STATS_CACHE_TTL_MINUTES = 10;

    // ==================== teacher 缓存 ====================

    public Object getFromCache(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void putIntoCache(String key, Object value) {
        redisTemplate.opsForValue().set(key, value, STATS_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 根据班级ID失效该班级的统计缓存
     */
    public void evictByClassId(Long classId) {
        if (classId == null) return;
        try {
            Set<String> keys = redisTemplate.keys(TEACHER_STATS_PREFIX + "class:" + classId + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("失效班级统计缓存: {} 条", keys.size());
            }
        } catch (Exception e) {
            log.warn("Redis 删除班级缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 根据院系ID失效该院系的统计缓存
     */
    public void evictByDeptId(Long deptId) {
        if (deptId == null) return;
        try {
            Set<String> keys = redisTemplate.keys(TEACHER_STATS_PREFIX + "dept:" + deptId + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("失效院系统计缓存: {} 条", keys.size());
            }
        } catch (Exception e) {
            log.warn("Redis 删除院系缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 根据学生信息同时失效班级和院系缓存
     */
    public void evictByStudent(Long classId, Long deptId) {
        evictByClassId(classId);
        evictByDeptId(deptId);
    }

    // ==================== analytics 缓存 ====================

    public Object getAnalyticsCache(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void putAnalyticsCache(String key, Object value) {
        redisTemplate.opsForValue().set(key, value, STATS_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    public void evictAnalyticsCache(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(ANALYTICS_STATS_PREFIX + pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("失效 analytics 缓存: {} 条", keys.size());
            }
        } catch (Exception e) {
            log.warn("Redis 删除 analytics 缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 失效所有 analytics 缓存（当就业数据变更时调用）
     */
    public void evictAllAnalytics() {
        evictAnalyticsCache("*");
    }

    public String analyticsKey(String suffix) {
        return ANALYTICS_STATS_PREFIX + suffix;
    }
}
