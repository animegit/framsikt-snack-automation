package com.framsikt.snack;

import java.util.UUID;

public class MockHRSystem implements HRSystem {
    @Override
    public String submitClaim(
        String employeeId,
        double amount,
        String currency,
        String description,
        String receiptUrl
    ) {
        return "HR-CLAIM-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
