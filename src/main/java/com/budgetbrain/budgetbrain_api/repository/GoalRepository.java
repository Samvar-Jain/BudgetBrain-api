package com.budgetbrain.budgetbrain_api.repository;

import com.budgetbrain.budgetbrain_api.model.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalRepository extends JpaRepository<Goal, Long> {
}