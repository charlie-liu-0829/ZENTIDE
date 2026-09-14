package com.zentide.controller;

import com.zentide.controller.ABaseController;
import com.zentide.entity.vo.ResponseVO;
import com.zentide.service.ZentideAdminGovernanceService;
import com.zentide.service.ZentideAdminCommunityService;
import com.zentide.service.ZentideGovernanceControlPlaneService;
import com.zentide.service.ZentideDirectMessageService;
import com.zentide.mapper.ZentideInterestMapper;
import com.zentide.exception.BusinessException;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

@RestController
@RequestMapping("/zentide/v1/admin/governance")
public class ZentideAdminGovernanceController extends ABaseController {
    private static final Logger log = LoggerFactory.getLogger(ZentideAdminGovernanceController.class);
    private final ZentideAdminGovernanceService service;
    private final ZentideGovernanceControlPlaneService controlPlane;
    private final ZentideAdminCommunityService communityService;
    private final ZentideDirectMessageService directMessages;
    private final ZentideInterestMapper interestMapper;
    public ZentideAdminGovernanceController(ZentideAdminGovernanceService service, ZentideGovernanceControlPlaneService controlPlane, ZentideAdminCommunityService communityService, ZentideDirectMessageService directMessages, ZentideInterestMapper interestMapper) { this.service = service; this.controlPlane=controlPlane; this.communityService=communityService; this.directMessages=directMessages; this.interestMapper=interestMapper; }

    @PostMapping("/review")
    public ResponseVO<?> review(@RequestBody Map<String,Object> payload) { getAdminAccount(); return getSuccessResponseVO(service.review(payload)); }
    @PostMapping("/feedback")
    public ResponseVO<?> feedback(@RequestBody Map<String,Object> payload) {
        String reviewer = getAdminAccount();
        Object content = payload.get("content_id");
        String action = String.valueOf(payload.getOrDefault("final_action", "")).trim().toLowerCase();
        if (!(content instanceof Number)) throw new BusinessException("缺少有效的帖子 ID");
        if (action.isBlank()) throw new BusinessException("请选择最终处理动作");
        long contentId = ((Number) content).longValue();
        log.info("治理反馈开始处理: contentId={}, finalAction={}, reviewer={}", contentId, action, reviewer);

        // 先执行管理员确认的内容动作，再记录反馈。这样即使反馈审计表暂时异常，
        // 管理员确认的隐藏动作也不会被静默跳过。
        boolean hidden = "temporary_hide".equals(action);
        if (hidden) communityService.updatePostStatus(contentId, "HIDDEN");
        controlPlane.saveFeedback(payload, reviewer);
        notifyAuthor(contentId, action, payload);
        log.info("治理反馈处理完成: contentId={}, finalAction={}, contentHidden={}", contentId, action, hidden);
        return getSuccessResponseVO(Map.of("accepted", true, "content_hidden", hidden, "content_id", contentId, "final_action", action));
    }

    private void notifyAuthor(long contentId, String action, Map<String, Object> payload) {
        String authorId = interestMapper.adminFindPostAuthorId(contentId);
        if (authorId == null || authorId.isBlank()) return;
        String reason = String.valueOf(payload.getOrDefault("reviewer_reason", "")).trim();
        String types = String.valueOf(payload.getOrDefault("corrected_violation_types", "[]"));
        String actionLabel = Map.of("allow", "允许发布", "flag", "标记关注", "manual_review", "进入人工审核", "temporary_hide", "暂时隐藏").getOrDefault(action, action);
        String body = "【社区治理处理结果】\n你的帖子（ID：" + contentId + "）已完成管理员审核。\n处理结果：" + actionLabel
                + "\n涉及风险：" + types + (reason.isBlank() ? "" : "\n处理说明：" + reason);
        try {
            directMessages.send("SYSTEM", authorId, body);
        } catch (RuntimeException ex) {
            // 私信是通知补充能力，不能阻断管理员已经确认的治理动作。
            log.warn("治理结果私信发送失败: contentId={}, authorId={}", contentId, authorId, ex);
        }
    }
    @PostMapping("/scan")
    public ResponseVO<?> scan(@RequestBody Map<String,Object> payload) { getAdminAccount(); return getSuccessResponseVO(service.scan(payload)); }
    @PostMapping("/results/pending")
    public ResponseVO<?> pendingResults() { getAdminAccount(); return getSuccessResponseVO(Map.of("results", controlPlane.pendingResults())); }
    @PostMapping("/rules/list")
    public ResponseVO<?> rules(@RequestBody Map<String,Object> payload) { getAdminAccount(); return getSuccessResponseVO(Map.of("rules", controlPlane.rules(0L))); }
    @PostMapping("/rules/all")
    public ResponseVO<?> allRules(@RequestBody Map<String,Object> payload) { getAdminAccount(); return getSuccessResponseVO(Map.of("rules", controlPlane.allRules(0L))); }
    @PostMapping("/rules")
    public ResponseVO<?> createRule(@RequestBody Map<String,Object> payload) { getAdminAccount(); return getSuccessResponseVO(controlPlane.saveRule(payload)); }
    @PostMapping("/rules/{ruleId}")
    public ResponseVO<?> updateRule(@PathVariable Long ruleId, @RequestBody Map<String,Object> payload) { getAdminAccount(); return getSuccessResponseVO(controlPlane.updateRule(ruleId, payload)); }
    @PostMapping("/rules/{ruleId}/delete")
    public ResponseVO<?> deleteRule(@PathVariable Long ruleId) { getAdminAccount(); return getSuccessResponseVO(controlPlane.deleteRule(ruleId)); }
    @PostMapping("/internal/rules")
    public ResponseVO<?> internalRules(@RequestHeader(value="X-Zentide-Agent-Token",required=false) String token,@RequestBody Map<String,Object> payload){checkInternal(token); Object scene=payload.get("scene_id"); long sceneId=scene instanceof Number n?n.longValue():0L; return getSuccessResponseVO(Map.of("rules",controlPlane.rules(sceneId)));}
    @PostMapping("/internal/result")
    public ResponseVO<?> internalResult(@RequestHeader(value="X-Zentide-Agent-Token",required=false) String token,@RequestBody Map<String,Object> payload){checkInternal(token);controlPlane.saveResult(payload);return getSuccessResponseVO(Map.of("saved",true));}
    private void checkInternal(String token){String expected=System.getenv("ZENTIDE_AGENT_INTERNAL_TOKEN");if(expected!=null&&!expected.isBlank()&&!expected.equals(token))throw new BusinessException("Agent 服务认证失败");}
}
