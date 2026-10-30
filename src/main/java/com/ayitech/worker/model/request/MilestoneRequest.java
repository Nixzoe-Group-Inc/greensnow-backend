package com.ayitech.worker.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilestoneRequest {

    private String title;
    private String description;
    private Integer displayOrder;
    // what this step is worth against the whole job. the weights are what turn a list of
    // ticked boxes into a percentage the employer can read, so a job whose weights do not
    // add up to a hundred reports progress that never reaches it
    private Integer weightPercentage;
}
