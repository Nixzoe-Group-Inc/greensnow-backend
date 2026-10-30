package com.greensnow.worker.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillListResponse {

    private String responseCode;
    private String responseMessage;
    private Integer totalSkills;
    private List<SkillDetail> skills;
}
