package org.example.couponapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.couponapi.controller.dto.CouponIssueRequestDto;
import org.example.couponcore.component.DistributeLockExecutor;
import org.example.couponcore.service.AsyncCouponIssueServiceV1;
import org.example.couponcore.service.AsyncCouponIssueServiceV2;
import org.example.couponcore.service.CouponIssueService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponIssueRequestService {

    private final CouponIssueService couponIssueService;
    private final AsyncCouponIssueServiceV1 couponIssueServiceV1;
    private final AsyncCouponIssueServiceV2 couponIssueServiceV2;

    public void issueRequestV1(CouponIssueRequestDto requestDto) {
        couponIssueService.issue(requestDto.couponId(), requestDto.userId());
        log.info(String.format("쿠폰 발급 완료. couponId: %s userId: %s", requestDto.couponId(), requestDto.userId()));
    }

    public void asyncIssueRequestV1(CouponIssueRequestDto requestDto) {
        couponIssueServiceV1.issue(requestDto.couponId(), requestDto.userId());
        log.info(String.format("쿠폰 발급 완료. couponId: %s userId: %s", requestDto.couponId(), requestDto.userId()));
    }

    public void asyncIssueRequestV2(CouponIssueRequestDto requestDto) {
        couponIssueServiceV2.issue(requestDto.couponId(), requestDto.userId());
        log.info(String.format("쿠폰 발급 완료. couponId: %s userId: %s", requestDto.couponId(), requestDto.userId()));
    }
}
