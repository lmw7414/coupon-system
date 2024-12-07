package org.example.couponcore.model;

import org.example.couponcore.exception.CouponIssueException;
import org.example.couponcore.exception.ErrorCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class CouponTest {

    @Test
    @DisplayName("발급 수량이 남아있다면 true를 반환")
    void availableIssueQuantity_1() {
        // Given
        Coupon coupon = Coupon.builder()
                .totalQuantity(100)
                .issuedQuantity(99)
                .build();
        // When
        boolean result = coupon.availableIssueQuantity();
        // Then
        Assertions.assertTrue(result);
    }

    @Test
    @DisplayName("발급 수량이 소진되었다면 false를 반환")
    void availableIssueQuantity_2() {
        // Given
        Coupon coupon = Coupon.builder()
                .totalQuantity(100)
                .issuedQuantity(100)
                .build();
        // When
        boolean result = coupon.availableIssueQuantity();
        // Then
        Assertions.assertFalse(result);
    }

    @Test
    @DisplayName("최대 발급 수량이 설정되지 않았다면 true를 반환")
    void availableIssueQuantity_3() {
        // Given
        Coupon coupon = Coupon.builder()
                .totalQuantity(null)
                .issuedQuantity(100)
                .build();
        // When
        boolean result = coupon.availableIssueQuantity();
        // Then
        Assertions.assertTrue(result);
    }

    @Test
    @DisplayName("발급 기간이 시작되었다면 true를 반환")
    void availableIssueDate_1() {
        // Given
        Coupon coupon = Coupon.builder()
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(1))
                .build();
        // When
        boolean result = coupon.availableIssueDate();
        // Then
        Assertions.assertTrue(result);
    }

    @Test
    @DisplayName("발급 기간이 시작되지 않았다면 false를 반환")
    void availableIssueDate_2() {
        // Given
        Coupon coupon = Coupon.builder()
                .dateIssueStart(LocalDateTime.now().plusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(2))
                .build();
        // When
        boolean result = coupon.availableIssueDate();
        // Then
        Assertions.assertFalse(result);
    }

    @Test
    @DisplayName("발급 기간이 종료되었다면 false를 반환")
    void availableIssueDate_3() {
        // Given
        Coupon coupon = Coupon.builder()
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().minusDays(2))
                .build();
        // When
        boolean result = coupon.availableIssueDate();
        // Then
        Assertions.assertFalse(result);
    }

    @Test
    @DisplayName("발급 수량과 발급 기간이 유효하다면 발급에 성공")
    void issue_1() {
        // Given
        Coupon coupon = Coupon.builder()
                .totalQuantity(100)
                .issuedQuantity(99)
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(2))
                .build();
        // When
        coupon.issue();
        // Then
        Assertions.assertEquals(coupon.getIssuedQuantity(), 100);
    }

    @Test
    @DisplayName("발급 수량을 초과하면 예외반환")
    void issue_2() {
        // Given
        Coupon coupon = Coupon.builder()
                .totalQuantity(100)
                .issuedQuantity(100)
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(2))
                .build();
        // When & Then
        CouponIssueException exception = Assertions.assertThrows(CouponIssueException.class, coupon::issue);
        Assertions.assertEquals(exception.getErrorCode(), ErrorCode.INVALID_COUPON_ISSUE_QUANTITY);
    }

    @Test
    @DisplayName("발급 기간이 유효하지 않다면 예외 반환")
    void issue_3() {
        // Given
        Coupon coupon = Coupon.builder()
                .totalQuantity(100)
                .issuedQuantity(99)
                .dateIssueStart(LocalDateTime.now().plusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(2))
                .build();
        // When & Then
        CouponIssueException exception = Assertions.assertThrows(CouponIssueException.class, coupon::issue);
        Assertions.assertEquals(exception.getErrorCode(), ErrorCode.INVALID_COUPON_ISSUE_DATE);
    }

    @Test
    @DisplayName("발급 기간이 종료되면 true를 반환한다.")
    void isIssueComplete_1() {
        // Given
        Coupon coupon = Coupon.builder()
                .totalQuantity(100)
                .issuedQuantity(0)
                .dateIssueStart(LocalDateTime.now().minusDays(2))
                .dateIssueEnd(LocalDateTime.now().minusDays(1))
                .build();
        // When
        boolean result = coupon.isIssueComplete();
        // Then
        Assertions.assertTrue(result);
    }

    @Test
    @DisplayName("잔여 발급 가능 수량이 없다면 true를 반환한다.")
    void isIssueComplete_2() {
        // Given
        Coupon coupon = Coupon.builder()
                .totalQuantity(100)
                .issuedQuantity(100)
                .dateIssueStart(LocalDateTime.now().minusDays(2))
                .dateIssueEnd(LocalDateTime.now().plusDays(1))
                .build();
        // When
        boolean result = coupon.isIssueComplete();
        // Then
        Assertions.assertTrue(result);
    }

    @Test
    @DisplayName("발급 기한과 수량이 유효하면 false를 반환한다.")
    void isIssueComplete_3() {
        // Given
        Coupon coupon = Coupon.builder()
                .totalQuantity(100)
                .issuedQuantity(99)
                .dateIssueStart(LocalDateTime.now().minusDays(2))
                .dateIssueEnd(LocalDateTime.now().plusDays(1))
                .build();
        // When
        boolean result = coupon.isIssueComplete();
        // Then
        Assertions.assertFalse(result);
    }
}