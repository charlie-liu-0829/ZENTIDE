package com.zentide.entity.po;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ZentideDirectMessage {
    private Long messageId;
    private String senderId;
    private String recipientId;
    private String body;
    private String status;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private String senderName;
    private String senderAvatar;
    private String peerId;
    private String peerName;
    private String peerAvatar;
    private Integer unreadCount;
}
