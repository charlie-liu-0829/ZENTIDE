package com.zentide.controller;

import com.zentide.annotation.GlobalInterceptor;
import com.zentide.controller.ABaseController;
import com.zentide.entity.dto.TokenUserInfoDTO;
import com.zentide.entity.vo.ResponseVO;
import com.zentide.exception.BusinessException;
import com.zentide.service.ZentideAgentGatewayService;
import com.zentide.service.ZentideInterestCommunityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import okhttp3.Call;

import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/zentide/v1/agent")
public class ZentideAgentController extends ABaseController {
    private final ZentideAgentGatewayService agentGateway;
    private final ZentideInterestCommunityService communityService;

    public ZentideAgentController(ZentideAgentGatewayService agentGateway,
                                  ZentideInterestCommunityService communityService) {
        this.agentGateway = agentGateway;
        this.communityService = communityService;
    }

    @PostMapping("/chat")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<Map<String, Object>> chat(@Valid @RequestBody AgentChatRequest request) {
        TokenUserInfoDTO user = getTokenUserInfo();
        String mode = request.mode().toLowerCase(Locale.ROOT);
        Long currentPostId = "post".equals(mode) ? request.currentPostId() : null;
        if ("post".equals(mode) && currentPostId == null) {
            throw new BusinessException("帖子模式必须指定帖子");
        }
        Set<String> visibilities = communityService.agentVisibilities(
                user.getUserId(), request.sceneId(), currentPostId);
        return getSuccessResponseVO(agentGateway.chat(
                user.getUserId(), request.sceneId(), currentPostId, request.currentPostIds(), mode,
                request.question(), request.conversationId(), visibilities));
    }

    @PostMapping(value = "/chat/stream", produces = "text/event-stream")
    @GlobalInterceptor(checkLogin = true)
    public SseEmitter chatStream(@Valid @RequestBody AgentChatRequest request) {
        TokenUserInfoDTO user = getTokenUserInfo();
        String mode = request.mode().toLowerCase(Locale.ROOT);
        Long currentPostId = "post".equals(mode) ? request.currentPostId() : null;
        if ("post".equals(mode) && currentPostId == null) {
            throw new BusinessException("帖子模式必须指定帖子");
        }
        Set<String> visibilities = communityService.agentVisibilities(
                user.getUserId(), request.sceneId(), currentPostId);
        SseEmitter emitter = new SseEmitter(75_000L);
        AtomicBoolean finished = new AtomicBoolean(false);
        emitter.onTimeout(() -> {
            finished.set(true);
            emitter.complete();
        });
        emitter.onError(error -> {
            finished.set(true);
            emitter.complete();
        });
        Call upstream = agentGateway.streamChat(user.getUserId(), request.sceneId(), currentPostId, request.currentPostIds(), mode,
                request.question(), request.conversationId(), visibilities,
                event -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name(String.valueOf(event.getOrDefault("event", "message")))
                                .data(event));
                        if (("done".equals(event.get("event")) || "error".equals(event.get("event")))
                                && finished.compareAndSet(false, true)) {
                            emitter.complete();
                        }
                    } catch (IOException error) {
                        emitter.completeWithError(error);
                    }
                }, error -> {
                    if (finished.compareAndSet(false, true)) emitter.completeWithError(error);
                });
        emitter.onCompletion(upstream::cancel);
        return emitter;
    }

    public record AgentChatRequest(
            @NotNull @Positive Long sceneId,
            @Positive Long currentPostId,
            List<@Positive Long> currentPostIds,
            @NotBlank @Pattern(regexp = "(?i)scene|post") String mode,
            @NotBlank @Size(max = 2000) String question,
            @Pattern(regexp = "[A-Za-z0-9_-]{8,80}") String conversationId) {
    }
}
