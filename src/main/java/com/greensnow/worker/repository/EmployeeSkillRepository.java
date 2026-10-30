package com.greensnow.worker.repository;

import com.greensnow.worker.model.entity.EmployeeSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeSkillRepository extends JpaRepository<EmployeeSkill, String> {

    List<EmployeeSkill> findByEmployeeProfileId(String employeeProfileId);

    Optional<EmployeeSkill> findByEmployeeProfileIdAndSkillId(String employeeProfileId, String skillId);

    boolean existsByEmployeeProfileIdAndSkillId(String employeeProfileId, String skillId);
}
