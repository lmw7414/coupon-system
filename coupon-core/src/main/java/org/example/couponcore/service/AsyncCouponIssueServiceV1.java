package org.example.couponcore.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.couponcore.component.DistributeLockExecutor;
import org.example.couponcore.exception.CouponIssueException;
import org.example.couponcore.model.Coupon;
import org.example.couponcore.repository.redis.RedisRepository;
import org.example.couponcore.repository.redis.dto.CouponIssueRequest;
import org.springframework.stereotype.Service;

import static org.example.couponcore.exception.ErrorCode.*;
import static org.example.couponcore.util.CouponRedisUtils.getIssueRequestKey;
import static org.example.couponcore.util.CouponRedisUtils.getIssueRequestQueueKey;

@Service
@RequiredArgsConstructor
public class AsyncCouponIssueServiceV1 {

    private final RedisRepository redisRepository;
    private final CouponIssueRedisService couponIssueRedisService;
    private final CouponIssueService couponIssueService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DistributeLockExecutor distributeLockExecutor;

    public void issue(long couponId, long userId) {
        Coupon coupon = couponIssueService.findCoupon(couponId);
        if(!coupon.availableIssueDate()) {
            throw new CouponIssueException(INVALID_COUPON_ISSUE_DATE,
                    String.format("발급 가능한 일자가 아닙니다. couponId: %s issueStart: %s issueEnd: %s", couponId, coupon.getDateIssueStart(), coupon.getDateIssueEnd())
            );
        }
        distributeLockExecutor.execute("lock_%s".formatted(couponId), 3000, 3000, () -> {
            if(!couponIssueRedisService.availableTotalIssueQuantity(coupon.getTotalQuantity() ,couponId)) {
                throw new CouponIssueException(INVALID_COUPON_ISSUE_QUANTITY,
                        String.format("발급 가능한 수량을 초과합니다. couponId: %s userId: %s", couponId, userId)
                );
            }
            if(!couponIssueRedisService.availableUserIssueQuantity(couponId, userId)) {
                throw new CouponIssueException(DUPLICATED_COUPON_ISSUE,
                        String.format("이미 발급 요청이 처리되었습니다. couponId: %s userId: %s", couponId, userId)
                );
            }
            issueRequest(couponId, userId);
        });
    }

    private void issueRequest(long couponId, long userId) {
        CouponIssueRequest issueRequest = new CouponIssueRequest(couponId, userId);
        try {
            String value = objectMapper.writeValueAsString(issueRequest);
            redisRepository.sAdd(getIssueRequestKey(couponId), String.valueOf(userId));
            redisRepository.rPush(getIssueRequestQueueKey(), value);
        } catch (JsonProcessingException e) {
            throw new CouponIssueException(FAIL_COUPON_ISSUE_REQUEST, "input: %s".formatted(issueRequest));
        }
    }
}
