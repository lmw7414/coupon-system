package org.example.couponcore.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.couponcore.exception.CouponIssueException;
import org.example.couponcore.exception.ErrorCode;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
@Table(name = "coupons")
public class Coupon extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*쿠폰명*/
    @Column(nullable = false)
    private String title;

    /*쿠폰 타입*/
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CouponType couponType;

    /*쿠폰 발급 최대 수량*/
    private Integer totalQuantity;

    /*발급된 쿠폰 수량*/
    @Column(nullable = false)
    private int issuedQuantity;

    /*할인 금액*/
    @Column(nullable = false)
    private int discountAmount;

    /*최소 사용 금액*/
    @Column(nullable = false)
    private int minAvailableAmount;

    /*발급 시작 일시*/
    @Column(nullable = false)
    private LocalDateTime dateIssueStart;

    /*발급 종료 일시*/
    @Column(nullable = false)
    private LocalDateTime dateIssueEnd;

    public boolean availableIssueQuantity() {
        if (totalQuantity == null) {
            return true;
        }
        return totalQuantity > issuedQuantity;
    }

    public boolean availableIssueDate() {
        LocalDateTime now = LocalDateTime.now();
        return dateIssueStart.isBefore(now) && dateIssueEnd.isAfter(now);
    }

    public void issue() {
        if(!availableIssueQuantity()) {
            throw new CouponIssueException(ErrorCode.INVALID_COUPON_ISSUE_QUANTITY, String.format("발급 가능한 수량을 초과합니다. total: %s, issued: %s",totalQuantity, issuedQuantity));
        }
        if(!availableIssueDate()) {
            throw new CouponIssueException(ErrorCode.INVALID_COUPON_ISSUE_DATE, String.format("발급 가능한 일자가 아닙니다. request: %s, issueStart: %s, issueEnd: %s",LocalDateTime.now(), dateIssueStart, dateIssueEnd));
        }
        issuedQuantity++;
    }
}
