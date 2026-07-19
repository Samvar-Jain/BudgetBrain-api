package com.budgetbrain.budgetbrain_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ClassifyResponse {

    @JsonProperty("transaction_count")
    private int transactionCount;

    private List<TransactionDto> transactions;

    @Data
    public static class TransactionDto {
        private String date;
        private String description;
        private Double amount;
        private String category;

        @JsonProperty("classification_method")
        private String classificationMethod;

        private Double confidence;
    }
}