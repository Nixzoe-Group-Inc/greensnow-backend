package com.greensnow.worker.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// one trade as the platform holds it. active is carried here where it is not on the browse the
// worker side reads, because the admin is the one person who needs to see a retired trade
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillDetail {

    private String skillId;
    private String name;
    private String slug;
    private String category;
    private String description;
    private String parentSkillId;
    private Boolean requiresCertification;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
