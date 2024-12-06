package org.example.couponapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.couponapi.controller.dto.CouponIssueRequestDto;
import org.example.couponcore.component.DistributeLockExecutor;
import org.example.couponcore.service.AsyncCouponIssueServiceV1;
import org.example.couponcore.service.CouponIssueService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponIssueRequestService {

    private final CouponIssueService couponIssueService;
    private final AsyncCouponIssueServiceV1 couponIssueServiceV1;
    private final DistributeLockExecutor distributeLockExecutor;

    public void issueRequestV1(CouponIssueRequestDto requestDto) {
//        distributeLockExecutor.execute("lock_" + requestDto.couponId(), 10000, 10000,() -> {
//            couponIssueService.issue(requestDto.couponId(), requestDto.userId());
//        });
        couponIssueService.issue(requestDto.couponId(), requestDto.userId());
        log.info(String.format("쿠폰 발급 완료. couponId: %s userId: %s", requestDto.couponId(), requestDto.userId()));
    }

    public void asyncIssueRequestV1(CouponIssueRequestDto requestDto) {
        couponIssueServiceV1.issue(requestDto.couponId(), requestDto.userId());
        log.info(String.format("쿠폰 발급 완료. couponId: %s userId: %s", requestDto.couponId(), requestDto.userId()));
    }
}
