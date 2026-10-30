package com.ayitech.worker.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingNotificationListResponse {

    private String responseCode;
    private String responseMessage;
    private Integer totalNotifications;
    private Integer unread;
    private List<TrackingNotificationDetail> notifications;
}
