package com.ayitech.worker.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;


@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "skills")
public class Skill extends BaseEntity {

    private String name;

    private String slug;

    private String category;

    private String description;

    private String parentSkillId;

    private boolean requiresCertification;

    private boolean active;

}
