package com.greensnow.worker.repository;

import com.greensnow.worker.model.entity.TrackingNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrackingNotificationRepository extends JpaRepository<TrackingNotification, String> {

    List<TrackingNotification> findByRecipientIdOrderByCreatedAtDesc(String recipientId);

    List<TrackingNotification> findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(String recipientId);

    Optional<TrackingNotification> findByIdAndRecipientId(String id, String recipientId);

    long countByRecipientIdAndReadAtIsNull(String recipientId);
}
