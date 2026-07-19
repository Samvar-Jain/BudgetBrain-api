package com.budgetbrain.budgetbrain_api.controller;

import com.budgetbrain.budgetbrain_api.dto.ClassifyResponse;
import com.budgetbrain.budgetbrain_api.model.Transaction;
import com.budgetbrain.budgetbrain_api.repository.TransactionRepository;
import com.budgetbrain.budgetbrain_api.service.InsightsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;


@RestController
@org.springframework.web.bind.annotation.CrossOrigin(origins = "http://localhost:5173")
public class UploadController {

    private final WebClient mlServiceWebClient;
    private final TransactionRepository transactionRepository;
    private final InsightsService insightsService;

    @Autowired
    public UploadController(WebClient mlServiceWebClient, TransactionRepository transactionRepository, InsightsService insightsService) {
        this.mlServiceWebClient = mlServiceWebClient;
        this.transactionRepository = transactionRepository;
        this.insightsService = insightsService;
    }

    @org.springframework.web.bind.annotation.GetMapping("/insights")
    public ResponseEntity<?> getInsights() {
        List<Transaction> allTransactions = transactionRepository.findAll();
        if (allTransactions.isEmpty()) {
            return ResponseEntity.badRequest().body("No transactions found. Upload a CSV first.");
        }
        String insight = insightsService.generateInsight(allTransactions);
        return ResponseEntity.ok(Map.of("insight", insight));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadCsv(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", file.getResource())
                    .filename(file.getOriginalFilename());

            ClassifyResponse classifyResponse = mlServiceWebClient.post()
                    .uri("/classify")
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(ClassifyResponse.class)
                    .block();

            if (classifyResponse == null || classifyResponse.getTransactions() == null) {
                return ResponseEntity.internalServerError().body("Empty response from ML service");
            }

            List<Transaction> saved = classifyResponse.getTransactions().stream()
                    .map(dto -> new Transaction(
                            null,
                            LocalDate.parse(dto.getDate()),
                            dto.getDescription(),
                            dto.getAmount(),
                            dto.getCategory(),
                            dto.getClassificationMethod(),
                            dto.getConfidence()
                    ))
                    .map(transactionRepository::save)
                    .toList();

            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Failed to process upload: " + e.getMessage());
        }
    }
}