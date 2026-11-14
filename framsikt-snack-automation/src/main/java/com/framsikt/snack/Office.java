package com.framsikt.snack;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Office {
    public String name;
    public String currency;
    public double monthlyBudget;
    public double currentSpend;
    public OfficeStatus status;
    public String assignedVolunteerId;
    public LocalDateTime assignedAt;
    public Set<String> declinedCurrentRound;
    public List<String> declineAuditLog;

    public Office(String name, String currency, double budget) {
        this.name = name;
        this.currency = currency;
        this.monthlyBudget = budget;
        this.currentSpend = 0.0;
        this.status = OfficeStatus.STOCKED;
        this.declinedCurrentRound = new HashSet<>();
        this.declineAuditLog = new ArrayList<>();
    }
}
