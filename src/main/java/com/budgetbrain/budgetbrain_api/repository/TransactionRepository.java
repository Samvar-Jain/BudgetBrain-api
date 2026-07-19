package com.budgetbrain.budgetbrain_api.repository;

import com.budgetbrain.budgetbrain_api.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
