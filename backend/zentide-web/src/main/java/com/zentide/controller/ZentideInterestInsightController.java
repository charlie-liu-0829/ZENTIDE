package com.zentide.controller;

import com.zentide.controller.ABaseController;
import com.zentide.entity.vo.ResponseVO;
import com.zentide.service.ZentideInterestInsightGatewayService;
import com.zentide.annotation.GlobalInterceptor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/zentide/v1/interest-insights")
public class ZentideInterestInsightController extends ABaseController {
    private final ZentideInterestInsightGatewayService gateway;

    public ZentideInterestInsightController(ZentideInterestInsightGatewayService gateway) { this.gateway = gateway; }

    @PostMapping("/generate")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<Map<String, Object>> generate(@Valid @RequestBody InsightRequest request) {
        return getSuccessResponseVO(gateway.insights(currentUser(), request.sceneId(), request.keywords(),
                request.seenPostIds(), null, request.maxItems()));
    }

    @GetMapping("/keywords")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<Map<String, Object>> keywords() { return getSuccessResponseVO(gateway.listKeywords(currentUser())); }

    @PostMapping("/keywords")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<Map<String, Object>> addKeyword(@Valid @RequestBody KeywordRequest request) {
        return getSuccessResponseVO(gateway.addKeyword(currentUser(), request.keyword()));
    }

    @PatchMapping("/keywords/{id}")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<Map<String, Object>> updateKeyword(@PathVariable @Positive Long id, @Valid @RequestBody KeywordStatusRequest request) {
        return getSuccessResponseVO(gateway.updateKeyword(currentUser(), id, request.enabled()));
    }

    @DeleteMapping("/keywords/{id}")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<Map<String, Object>> deleteKeyword(@PathVariable @Positive Long id) {
        return getSuccessResponseVO(gateway.deleteKeyword(currentUser(), id));
    }

    @PostMapping("/feedback")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<Boolean> feedback(@Valid @RequestBody FeedbackRequest request) {
        gateway.feedback(currentUser(), request.itemId(), request.action(), request.item());
        return getSuccessResponseVO(true);
    }

    @GetMapping("/saved")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<Map<String, Object>> saved() { return getSuccessResponseVO(gateway.saved(currentUser())); }

    @DeleteMapping("/saved/{itemId}")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<Map<String, Object>> unsave(@PathVariable String itemId) { return getSuccessResponseVO(gateway.unsave(currentUser(), itemId)); }

    private String currentUser() { return getTokenUserInfo().getUserId(); }

    public record InsightRequest(@Positive Long sceneId, @Size(max = 30) List<@NotBlank @Size(max = 80) String> keywords,
                                 @Size(max = 500) List<@Positive Long> seenPostIds,
                                 @NotNull @Positive @Max(30) Integer maxItems) {
        public InsightRequest { maxItems = maxItems == null ? 10 : maxItems; }
    }
    public record KeywordRequest(@NotBlank @Size(max = 80) String keyword) { }
    public record KeywordStatusRequest(@NotNull Boolean enabled) { }
    public record FeedbackRequest(@NotBlank @Size(max = 100) String itemId,
                                  @NotBlank @Size(max = 30) String action,
                                  Map<String, Object> item) { }
}
