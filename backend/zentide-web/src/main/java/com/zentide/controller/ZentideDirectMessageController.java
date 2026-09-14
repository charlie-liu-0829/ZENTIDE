package com.zentide.controller;

import com.zentide.annotation.GlobalInterceptor;
import com.zentide.controller.ABaseController;
import com.zentide.entity.vo.ResponseVO;
import com.zentide.service.ZentideDirectMessageService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/zentide/v1/messages")
@Validated
@GlobalInterceptor(checkLogin = true)
public class ZentideDirectMessageController extends ABaseController {
    private final ZentideDirectMessageService service;

    public ZentideDirectMessageController(ZentideDirectMessageService service) {
        this.service = service;
    }

    @PostMapping("/conversations")
    public ResponseVO<?> conversations(@RequestParam(required = false) @Min(1) @Max(100) Integer limit) {
        return getSuccessResponseVO(service.conversations(currentUserId(), limit));
    }

    @PostMapping("/with/{peerId}")
    public ResponseVO<?> messages(@PathVariable String peerId, @RequestParam(required = false) @Min(1) @Max(300) Integer limit) {
        return getSuccessResponseVO(service.messages(currentUserId(), peerId, limit));
    }

    @PostMapping("/with/{peerId}/send")
    public ResponseVO<?> send(@PathVariable String peerId, @RequestParam @Size(min = 1, max = 2000) String body) {
        return getSuccessResponseVO(service.send(currentUserId(), peerId, body));
    }

    @PostMapping("/unread-count")
    public ResponseVO<?> unreadCount() {
        return getSuccessResponseVO(service.unreadCount(currentUserId()));
    }

    @PostMapping("/users/search")
    public ResponseVO<?> searchUsers(@RequestParam @Size(min = 1, max = 40) String query) {
        return getSuccessResponseVO(service.searchUsers(currentUserId(), query));
    }

    private String currentUserId() {
        return getTokenUserInfo().getUserId();
    }
}
