package com.ayitech.worker.model.entity;

import com.ayitech.worker.model.enums.TrackingNotificationType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tracking_notifications")
public class TrackingNotification extends BaseEntity {

    private String recipientId;

    private String jobExecutionId;

    @Enumerated(EnumType.STRING)
    private TrackingNotificationType notificationType;

    private String title;

    private String body;

    private String payload;

    private String actionUrl;

    private boolean requiresAction;

    private boolean pushed;

    private LocalDateTime pushedAt;

    private LocalDateTime readAt;

}
