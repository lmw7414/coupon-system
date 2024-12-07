package org.example.couponcore.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.couponcore.TestConfig;
import org.example.couponcore.exception.CouponIssueException;
import org.example.couponcore.model.Coupon;
import org.example.couponcore.model.CouponType;
import org.example.couponcore.repository.mysql.CouponJpaRepository;
import org.example.couponcore.repository.redis.dto.CouponIssueRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.stream.IntStream;

import static org.example.couponcore.exception.ErrorCode.*;
import static org.example.couponcore.util.CouponRedisUtils.getIssueRequestKey;
import static org.example.couponcore.util.CouponRedisUtils.getIssueRequestQueueKey;

class AsyncCouponIssueServiceV2Test extends TestConfig {
    @Autowired
    AsyncCouponIssueServiceV2 sut;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @Autowired
    CouponJpaRepository couponJpaRepository;

    @BeforeEach
    void clear() {
        Collection<String> redisKeys = redisTemplate.keys("*");
        redisTemplate.delete(redisKeys);
    }

    @Test
    @DisplayName("쿠폰 발급 - 쿠폰이 존재하지 않는다면 예외를 반환한다.")
    void issue_1() {
        // Given
        long couponId = 1;
        long userId = 1;
        // When & Then
        CouponIssueException exception = Assertions.assertThrows(CouponIssueException.class, () -> sut.issue(couponId, userId));
        Assertions.assertEquals(COUPON_NOT_EXIST, exception.getErrorCode());
    }

    @Test
    @DisplayName("쿠폰 발급 - 발급 가능 수량이 존재하지 않는다면 예외를 반환한다.")
    void issue_2() {
        // Given
        long userId = 1000;
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(1))
                .build();
        couponJpaRepository.save(coupon);
        IntStream.range(0, coupon.getTotalQuantity()).forEach(idx -> redisTemplate.opsForSet().add(getIssueRequestKey(coupon.getId()), String.valueOf(idx)));
        // When & Then
        CouponIssueException exception = Assertions.assertThrows(CouponIssueException.class, () -> sut.issue(coupon.getId(), userId));
        Assertions.assertEquals(INVALID_COUPON_ISSUE_QUANTITY, exception.getErrorCode());
    }

    @Test
    @DisplayName("쿠폰 발급 - 이미 발급된 유저라면 예외를 반환한다.")
    void issue_3() {
        // Given
        long userId = 1;
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(1))
                .build();
        couponJpaRepository.save(coupon);
        redisTemplate.opsForSet().add(getIssueRequestKey(coupon.getId()), String.valueOf(userId));
        // When & Then
        CouponIssueException exception = Assertions.assertThrows(CouponIssueException.class, () -> sut.issue(coupon.getId(), userId));
        Assertions.assertEquals(DUPLICATED_COUPON_ISSUE, exception.getErrorCode());
    }

    @Test
    @DisplayName("쿠폰 발급 - 발급 기한이 유효하지 않다면 예외를 반환한다.")
    void issue_4() {
        // Given
        long userId = 1;
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .dateIssueStart(LocalDateTime.now().minusDays(3))
                .dateIssueEnd(LocalDateTime.now().minusDays(1))
                .build();
        couponJpaRepository.save(coupon);
        redisTemplate.opsForSet().add(getIssueRequestKey(coupon.getId()), String.valueOf(userId));
        // When & Then
        CouponIssueException exception = Assertions.assertThrows(CouponIssueException.class, () -> sut.issue(coupon.getId(), userId));
        Assertions.assertEquals(INVALID_COUPON_ISSUE_DATE, exception.getErrorCode());
    }

    @Test
    @DisplayName("쿠폰 발급 - 쿠폰 발급을 기록한다.")
    void issue_5() {
        // Given
        long userId = 1;
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(1))
                .build();
        couponJpaRepository.save(coupon);
        // When
        sut.issue(coupon.getId(), userId);

        // Then
        Boolean isSaved = redisTemplate.opsForSet().isMember(getIssueRequestKey(coupon.getId()), String.valueOf(userId));
        Assertions.assertTrue(isSaved);
    }

    @Test
    @DisplayName("쿠폰 발급 - 쿠폰 발급요청이 성공하면 큐에 적재된다.")
    void issue_6() throws JsonProcessingException {
        // Given
        long userId = 1;
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(1))
                .build();
        couponJpaRepository.save(coupon);
        CouponIssueRequest request = new CouponIssueRequest(coupon.getId(), userId);
        // When
        sut.issue(coupon.getId(), userId);

        // Then
        String savedIssueRequest = redisTemplate.opsForList().leftPop(getIssueRequestQueueKey());
        Assertions.assertEquals(new ObjectMapper().writeValueAsString(request), savedIssueRequest);
    }

}