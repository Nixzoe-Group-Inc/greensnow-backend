package com.greensnow.worker.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.greensnow.worker.model.enums.TrackingNotificationType;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingNotificationDetail {

    private String notificationId;
    private String executionId;
    private TrackingNotificationType notificationType;
    private String title;
    private String body;
    private String actionUrl;
    private Boolean requiresAction;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
