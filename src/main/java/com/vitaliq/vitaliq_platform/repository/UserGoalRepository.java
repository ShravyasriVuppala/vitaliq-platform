package com.vitaliq.vitaliq_platform.repository;

import com.vitaliq.vitaliq_platform.model.user.UserGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserGoalRepository extends JpaRepository<UserGoal, UUID> {
    Optional<UserGoal> findByUserIdAndIsActiveTrue(UUID userId);
    List<UserGoal> findByUserIdOrderByCreatedAtDesc(UUID userId);
}