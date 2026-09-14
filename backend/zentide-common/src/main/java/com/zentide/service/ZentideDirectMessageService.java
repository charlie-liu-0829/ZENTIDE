package com.zentide.service;

import com.zentide.entity.po.ZentideDirectMessage;
import com.zentide.exception.BusinessException;
import com.zentide.mapper.ZentideDirectMessageMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.time.LocalDateTime;

@Service
public class ZentideDirectMessageService {
    private final ZentideDirectMessageMapper mapper;

    public ZentideDirectMessageService(ZentideDirectMessageMapper mapper) {
        this.mapper = mapper;
    }

    public List<ZentideDirectMessage> conversations(String userId, Integer requestedLimit) {
        return mapper.listConversations(userId, boundedLimit(requestedLimit, 50));
    }

    @Transactional
    public List<ZentideDirectMessage> messages(String userId, String peerId, Integer requestedLimit) {
        requirePeer(userId, peerId);
        mapper.markConversationRead(userId, peerId);
        List<ZentideDirectMessage> messages = mapper.listMessages(userId, peerId, boundedLimit(requestedLimit, 200));
        Collections.reverse(messages);
        return messages;
    }

    @Transactional
    public ZentideDirectMessage send(String senderId, String recipientId, String body) {
        requirePeer(senderId, recipientId);
        String cleanBody = body == null ? "" : body.trim();
        if (cleanBody.isEmpty() || cleanBody.length() > 2000) throw new BusinessException("私信内容需要是 1 到 2000 个字符");
        ZentideDirectMessage message = new ZentideDirectMessage();
        message.setSenderId(senderId);
        message.setRecipientId(recipientId);
        message.setBody(cleanBody);
        message.setStatus("SENT");
        if (mapper.insertMessage(message) != 1) throw new BusinessException("私信发送失败，请稍后重试");
        message.setCreatedAt(LocalDateTime.now());
        return message;
    }

    public int unreadCount(String userId) {
        return mapper.countUnread(userId);
    }

    public List<ZentideDirectMessage> searchUsers(String userId, String query) {
        String cleanQuery = query == null ? "" : query.trim();
        if (cleanQuery.isEmpty() || cleanQuery.length() > 40) throw new BusinessException("请输入 1 到 40 个字符搜索用户");
        return mapper.searchUsers(userId, cleanQuery, 12);
    }

    private void requirePeer(String userId, String peerId) {
        if (peerId == null || peerId.isBlank() || userId.equals(peerId)) throw new BusinessException("不能给自己发送私信");
        if (mapper.countUser(peerId) != 1) throw new BusinessException("用户不存在");
    }

    private int boundedLimit(Integer requested, int fallback) {
        return requested == null ? fallback : Math.max(1, Math.min(300, requested));
    }
}
