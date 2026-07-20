package com.budgetbrain.budgetbrain_api.controller;

import com.budgetbrain.budgetbrain_api.model.Goal;
import com.budgetbrain.budgetbrain_api.repository.GoalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/goals")
@CrossOrigin(origins = {"http://localhost:5173", "https://budgetbrain-frontend.vercel.app"})
public class GoalController {

    private final GoalRepository goalRepository;

    @Autowired
    public GoalController(GoalRepository goalRepository) {
        this.goalRepository = goalRepository;
    }

    @GetMapping
    public List<Goal> getAllGoals() {
        return goalRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Goal> createGoal(@RequestBody Goal goal) {
        goal.setId(null); // ensure it's treated as a new entity, not an update
        Goal saved = goalRepository.save(goal);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateGoal(@PathVariable Long id, @RequestBody Goal updatedGoal) {
        return goalRepository.findById(id)
                .map(existing -> {
                    existing.setName(updatedGoal.getName());
                    existing.setTargetAmount(updatedGoal.getTargetAmount());
                    existing.setCurrentAmount(updatedGoal.getCurrentAmount());
                    existing.setDeadline(updatedGoal.getDeadline());
                    return ResponseEntity.ok(goalRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGoal(@PathVariable Long id) {
        if (!goalRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        goalRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}