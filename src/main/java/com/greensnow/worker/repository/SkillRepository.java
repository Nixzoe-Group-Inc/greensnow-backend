package com.greensnow.worker.repository;

import com.greensnow.worker.model.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, String> {

    List<Skill> findByActiveTrue();

    List<Skill> findByCategoryIgnoreCaseAndActiveTrue(String category);

    List<Skill> findByNameContainingIgnoreCaseAndActiveTrue(String name);

    // the admin's own listing, which deliberately does NOT filter on active. a retired trade is
    // still on every profile and every job that was built around it, so the one person who can
    // bring it back has to be able to see it
    List<Skill> findAllByOrderByNameAsc();

    List<Skill> findByCategoryIgnoreCaseOrderByNameAsc(String category);

    boolean existsByNameIgnoreCase(String name);

    boolean existsBySlugIgnoreCase(String slug);

    Optional<Skill> findBySlugIgnoreCase(String slug);
}
