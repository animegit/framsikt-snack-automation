package com.framsikt.snack;

public interface HRSystem {
    String submitClaim(
        String employeeId,
        double amount,
        String currency,
        String description,
        String receiptUrl
    );
}
