package com.vitaliq.vitaliq_platform.repository;

import com.vitaliq.vitaliq_platform.model.master.HealthCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface HealthConditionRepository extends JpaRepository<HealthCondition, UUID> {
}