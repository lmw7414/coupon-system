package org.example.couponcore.service;

import org.example.couponcore.TestConfig;
import org.example.couponcore.exception.CouponIssueException;
import org.example.couponcore.exception.ErrorCode;
import org.example.couponcore.model.Coupon;
import org.example.couponcore.model.CouponIssue;
import org.example.couponcore.model.CouponType;
import org.example.couponcore.repository.mysql.CouponIssueJpaRepository;
import org.example.couponcore.repository.mysql.CouponIssueRepository;
import org.example.couponcore.repository.mysql.CouponJpaRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

class CouponIssueServiceTest extends TestConfig {

    @Autowired
    CouponIssueService sut;

    @Autowired
    CouponIssueJpaRepository couponIssueJpaRepository;

    @Autowired
    CouponIssueRepository couponIssueRepository;

    @Autowired
    CouponJpaRepository couponJpaRepository;

    @BeforeEach
    void clean() {
        couponJpaRepository.deleteAllInBatch();
        couponIssueJpaRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("쿠폰 발급 내역이 존재하면 예외를 반환한다")
    void saveCouponIssue_1() {
        // Given
        CouponIssue issue = CouponIssue.builder()
                .couponId(1L)
                .userId(1L)
                .build();
        couponIssueJpaRepository.save(issue);
        // When & Then
        CouponIssueException exception = Assertions.assertThrows(CouponIssueException.class, () ->
                sut.saveCouponIssue(issue.getCouponId(), issue.getUserId())
        );
        Assertions.assertEquals(exception.getErrorCode(), ErrorCode.DUPLICATED_COUPON_ISSUE);
    }

    @Test
    @DisplayName("쿠폰 발급 내역이 존재하지 않는다면 쿠폰 발급")
    void saveCouponIssue_2() {
        // Given
        long couponId = 1L;
        long userId = 1L;
        // When
        CouponIssue result = sut.saveCouponIssue(couponId, userId);
        // Then
        Assertions.assertTrue(couponIssueJpaRepository.findById(result.getId()).isPresent());
    }

    @Test
    @DisplayName("발급 수량, 기한, 중복 발급 문제가 없다면 쿠폰을 발급")
    void issue_1() {
        // Given
        long userId = 1L;
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(100)
                .issuedQuantity(0)
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(1))
                .build();
        couponJpaRepository.save(coupon);
        // When
        sut.issue(coupon.getId(), userId);
        // Then
        Coupon couponResult = couponJpaRepository.findById(coupon.getId()).get();
        Assertions.assertEquals(couponResult.getIssuedQuantity(), 1);
        CouponIssue couponIssueResult = couponIssueRepository.findFirstCouponIssue(coupon.getId(), userId);
        Assertions.assertNotNull(couponIssueResult);
    }

    @Test
    @DisplayName("발급 수량에 문제가 있다면 예외 반환")
    void issue_2() {
        // Given
        long userId = 1L;
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(100)
                .issuedQuantity(100)
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(1))
                .build();
        couponJpaRepository.save(coupon);
        // When & Then
        CouponIssueException exception = Assertions.assertThrows(CouponIssueException.class, () -> sut.issue(coupon.getId(), userId));
        Assertions.assertEquals(exception.getErrorCode(), ErrorCode.INVALID_COUPON_ISSUE_QUANTITY);
    }

    @Test
    @DisplayName("발급 기한에 문제가 있다면 예외 반환")
    void issue_3() {
        // Given
        long userId = 1L;
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(100)
                .issuedQuantity(0)
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().minusDays(2))
                .build();
        couponJpaRepository.save(coupon);
        // When & Then
        CouponIssueException exception = Assertions.assertThrows(CouponIssueException.class, () -> sut.issue(coupon.getId(), userId));
        Assertions.assertEquals(exception.getErrorCode(), ErrorCode.INVALID_COUPON_ISSUE_DATE);
    }

    @Test
    @DisplayName("중복 발급 검증에 문제가 있다면 예외 반환")
    void issue_4() {
        // Given
        long userId = 1L;
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(100)
                .issuedQuantity(0)
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(2))
                .build();
        couponJpaRepository.save(coupon);

        CouponIssue couponIssue = CouponIssue.builder()
                .couponId(coupon.getId())
                .userId(userId)
                .build();
        couponIssueJpaRepository.save(couponIssue);
        // When & Then
        CouponIssueException exception = Assertions.assertThrows(CouponIssueException.class, () -> sut.issue(coupon.getId(), userId));
        Assertions.assertEquals(exception.getErrorCode(), ErrorCode.DUPLICATED_COUPON_ISSUE);
    }

    @Test
    @DisplayName("쿠폰이 존재하지 않는다면 예외 반환")
    void issue_5() {
        // Given
        long userId = 1L;
        long couponId = 1L;
        // When & Then
        CouponIssueException exception = Assertions.assertThrows(CouponIssueException.class, () -> sut.issue(couponId, userId));
        Assertions.assertEquals(exception.getErrorCode(), ErrorCode.COUPON_NOT_EXIST);
    }

}