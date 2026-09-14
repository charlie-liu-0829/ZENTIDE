package com.zentide.controller;

import com.zentide.annotation.GlobalInterceptor;
import com.zentide.controller.ABaseController;
import com.zentide.entity.vo.ResponseVO;
import com.zentide.service.ZentideGovernanceGatewayService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/zentide/v1/governance")
public class ZentideGovernanceController extends ABaseController {
    private final ZentideGovernanceGatewayService gateway;
    public ZentideGovernanceController(ZentideGovernanceGatewayService gateway) { this.gateway = gateway; }

    @PostMapping("/review")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<Map<String, Object>> review(@Valid @RequestBody GovernanceRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>(request.payload());
        return getSuccessResponseVO(gateway.review(payload));
    }

    @PostMapping("/scan")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<Map<String, Object>> scan(@Valid @RequestBody ScanRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("scene_id", request.sceneId());
        payload.put("max_posts", request.maxPosts());
        return getSuccessResponseVO(gateway.scan(payload));
    }
    @GetMapping("/rules")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<Map<String, Object>> rules(@RequestParam @Positive Long sceneId) {
        return getSuccessResponseVO(gateway.rules(Map.of("scene_id", sceneId)));
    }
    @PostMapping("/rules")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<Map<String, Object>> createRule(@Valid @RequestBody RuleRequest request) {
        Map<String,Object> payload = new LinkedHashMap<>(); payload.put("scene_id", request.sceneId()); payload.put("violation_type", request.violationType()); payload.put("keywords", request.keywords()); payload.put("severity", request.severity());
        return getSuccessResponseVO(gateway.createRule(payload));
    }

    @PostMapping("/feedback")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<Map<String, Object>> feedback(@Valid @RequestBody FeedbackRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("content_id", request.contentId()); payload.put("agent_result_id", request.agentResultId());
        payload.put("final_action", request.finalAction()); payload.put("accepted_agent_advice", request.acceptedAgentAdvice());
        payload.put("corrected_violation_types", request.correctedViolationTypes()); payload.put("reviewer_reason", request.reviewerReason());
        return getSuccessResponseVO(gateway.feedback(payload));
    }

    public record GovernanceRequest(@Positive Long contentId, @Positive Long sceneId, @NotBlank @Size(max=220) String title,
                                     @NotBlank @Size(max=50000) String content, List<Map<String,Object>> rules, List<Map<String,Object>> cases) {
        Map<String,Object> payload() { Map<String,Object> p=new LinkedHashMap<>(); p.put("content_id",contentId); p.put("scene_id",sceneId); p.put("title",title); p.put("content",content); p.put("rules",rules==null?List.of():rules); p.put("cases",cases==null?List.of():cases); return p; }
    }
    public record ScanRequest(@Positive Long sceneId, @Positive Integer maxPosts) { }
    public record RuleRequest(@Positive Long sceneId, @NotBlank String violationType, @Size(min=1) List<String> keywords, @NotBlank String severity) { }
    public record FeedbackRequest(@Positive Long contentId, @NotBlank String agentResultId, @NotBlank String finalAction,
                                  @NotNull Boolean acceptedAgentAdvice, List<String> correctedViolationTypes, @NotBlank String reviewerReason) { }
}
