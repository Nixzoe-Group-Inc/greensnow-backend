package com.ayitech.worker.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// a trade in the platform's catalogue. every skill a worker claims and every skill a job asks for
// is one of these, which is why only the platform writes them - a free text trade would mean a
// plumber and a Plumber never match the same job
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillRequest {

    private String name;
    private String slug;
    private String category;
    private String description;
    private String parentSkillId;

    // a licensed trade is licensed. setting this makes the platform ask every worker claiming it
    // for the certificate, which is the one thing an employer cannot check for themselves on site
    private Boolean requiresCertification;

    // active is deliberately absent. retiring a trade is its own route, for the same reason
    // closing a job is: it is a decision about something already in use, not a field on an edit
    // that could be cleared by leaving it out
}
