package com.ayitech.worker.model.enums;

public enum JobStatus {
    DRAFT,
    PENDING_APPROVAL,
    // sent back to the employer to fix rather than turned down. the approval vocabulary has
    // always had it; without a job state to match, a reviewer asking for a changed rate would
    // have to record it as REJECTED, which reads to the employer as "we will not run this"
    CHANGES_REQUESTED,
    APPROVED,
    REJECTED,
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    CLOSED;

    public boolean isPickable() {
        return this == APPROVED;
    }
}
