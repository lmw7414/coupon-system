package org.example.couponcore.service;

import org.example.couponcore.TestConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collection;
import java.util.stream.IntStream;

import static org.example.couponcore.util.CouponRedisUtils.getIssueRequestKey;

class CouponIssueRedisServiceTest extends TestConfig {

    @Autowired
    CouponIssueRedisService sut;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void clear() {
        Collection<String> redisKeys = redisTemplate.keys("*");
        redisTemplate.delete(redisKeys);
    }

    @Test
    @DisplayName("쿠폰 수량 검증 - 발급 가능 수량이 존재하면 true를 반환한다.")
    void availableTotalIssueQuantity_1() {
        // Given
        int totalIssueQuantity = 10;
        long couponId = 1;
        // When
        boolean result = sut.availableTotalIssueQuantity(totalIssueQuantity, couponId);
        // Then
        Assertions.assertTrue(result);
    }

    @Test
    @DisplayName("쿠폰 수량 검증 - 발급 가능 수량이 모두 소진되면 false를 반환한다.")
    void availableTotalIssueQuantity_2() {
        // Given
        int totalIssueQuantity = 10;
        long couponId = 1;
        IntStream.range(0, totalIssueQuantity).forEach(userId -> redisTemplate.opsForSet().add(getIssueRequestKey(couponId), String.valueOf(userId)));
        // When
        boolean result = sut.availableTotalIssueQuantity(totalIssueQuantity, couponId);
        // Then
        Assertions.assertFalse(result);
    }

    @Test
    @DisplayName("쿠폰 중복 발급 검증 - 발급된 내역에 유저가 존재하지 않으면 true 반환")
    void availableUserIssueQuantity_1() {
        // Given
        long couponId = 1;
        long userId = 1;
        // When
        boolean result = sut.availableUserIssueQuantity(couponId, userId);
        // Then
        Assertions.assertTrue(result);
    }

    @Test
    @DisplayName("쿠폰 중복 발급 검증 - 발급된 내역에 유저가 존재하면 false 반환")
    void availableUserIssueQuantity_2() {
        // Given
        long couponId = 1;
        long userId = 1;
        redisTemplate.opsForSet().add(getIssueRequestKey(couponId), String.valueOf(userId));
        // When
        boolean result = sut.availableUserIssueQuantity(couponId, userId);
        // Then
        Assertions.assertFalse(result);
    }

}