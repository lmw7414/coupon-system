package org.example.couponcore.service;

import lombok.RequiredArgsConstructor;
import org.example.couponcore.exception.CouponIssueException;
import org.example.couponcore.exception.ErrorCode;
import org.example.couponcore.model.Coupon;
import org.example.couponcore.model.CouponIssue;
import org.example.couponcore.repository.mysql.CouponIssueJpaRepository;
import org.example.couponcore.repository.mysql.CouponIssueRepository;
import org.example.couponcore.repository.mysql.CouponJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.example.couponcore.exception.ErrorCode.COUPON_NOT_EXIST;
import static org.example.couponcore.exception.ErrorCode.DUPLICATED_COUPON_ISSUE;

@Service
@RequiredArgsConstructor
public class CouponIssueService {
    private final CouponJpaRepository couponJpaRepository;
    private final CouponIssueJpaRepository couponIssueJpaRepository;
    private final CouponIssueRepository couponIssueRepository;

    @Transactional
    public void issue(long couponId, long userId) {
        Coupon coupon = findCoupon(couponId);
        coupon.issue();
        saveCouponIssue(couponId, userId);
    }

    /* 이 구조의 문제점
    @Transactional
    public void issue(long couponId, long userId) {
        synchronized (this){
            Coupon coupon = findCoupon(couponId);
            coupon.issue();
            saveCouponIssue(couponId, userId);
        }
    }
    1. 트랜잭션 시작
    2. lock 획득
    3. 쿠폰 발급 로직 실행(저장)
    4. lock 반납
    5. 트랜잭션 커밋
    -> lock 반납함과 동시에 다른 쓰레드에서 lock을 획득하기 때문에 동시성 문제가 해결되지 않는다.

    * 해결 방법 *
    1. lock 획득
    2. 트랜잭션 시작
    3. 쿠폰 발급 로직
    4. 트랜잭션 종료
    5. lock 반납
     */

    @Transactional(readOnly = true)
    public Coupon findCoupon(long couponId) {
        return couponJpaRepository.findById(couponId)
                .orElseThrow(() -> new CouponIssueException(COUPON_NOT_EXIST, String.format("쿠폰 정책이 존재하지 않습니다. %s", couponId)));
    }

    @Transactional
    public CouponIssue saveCouponIssue(long couponId, long userId) {
        checkAlreadyIssuance(couponId, userId);
        CouponIssue issue = CouponIssue.builder()
                .couponId(couponId)
                .userId(userId)
                .build();
        return couponIssueJpaRepository.save(issue);
    }

    private void checkAlreadyIssuance(long couponId, long userId) {
        CouponIssue issue = couponIssueRepository.findFirstCouponIssue(couponId, userId);
        if (issue != null) {
            throw new CouponIssueException(DUPLICATED_COUPON_ISSUE, String.format("이미 발급된 쿠폰입니다. user_id: %s, coupon_id: %s", userId, couponId));
        }
    }

}
