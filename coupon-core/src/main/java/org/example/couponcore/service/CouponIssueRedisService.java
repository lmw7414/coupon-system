package org.example.couponcore.service;

import lombok.RequiredArgsConstructor;
import org.example.couponcore.exception.CouponIssueException;
import org.example.couponcore.repository.redis.RedisRepository;
import org.example.couponcore.repository.redis.dto.CouponRedisEntity;
import org.springframework.stereotype.Service;

import static org.example.couponcore.exception.ErrorCode.DUPLICATED_COUPON_ISSUE;
import static org.example.couponcore.exception.ErrorCode.INVALID_COUPON_ISSUE_QUANTITY;
import static org.example.couponcore.util.CouponRedisUtils.getIssueRequestKey;

@Service
@RequiredArgsConstructor
public class CouponIssueRedisService {
    private final RedisRepository redisRepository;

    public void checkCouponIssueQuantity(CouponRedisEntity coupon, long userId) {
        if(!availableTotalIssueQuantity(coupon.totalQuantity() ,coupon.id())) {
            throw new CouponIssueException(INVALID_COUPON_ISSUE_QUANTITY,
                    String.format("발급 가능한 수량을 초과합니다. couponId: %s userId: %s", coupon.id(), userId)
            );
        }
        if(!availableUserIssueQuantity(coupon.id(), userId)) {
            throw new CouponIssueException(DUPLICATED_COUPON_ISSUE,
                    String.format("이미 발급 요청이 처리되었습니다. couponId: %s userId: %s", coupon.id(), userId)
            );
        }
    }

    public boolean availableTotalIssueQuantity(Integer totalQuantity, long couponId) {
        if(totalQuantity == null) return true;
        String key = getIssueRequestKey(couponId);
        return totalQuantity > redisRepository.sCard(key);
    }

    public boolean availableUserIssueQuantity(long couponId, long userId) {
        String key = getIssueRequestKey(couponId);
        return !redisRepository.sIsMember(key, String.valueOf(userId));
    }
}
