package com.zentide.entity.po;
import lombok.Data;
import java.time.LocalDateTime;
@Data public class ZentideInterestEvent { private Long eventId; private Long hubId; private String hubName; private Long entityId; private String title; private String description; private LocalDateTime startsAt; private LocalDateTime endsAt; private String venue; private String sourceUrl; private String status; private String attendanceStatus; private Integer attendeeCount; }
