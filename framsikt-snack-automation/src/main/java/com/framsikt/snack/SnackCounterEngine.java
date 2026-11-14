package com.framsikt.snack;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SnackCounterEngine {
    public Map<String, Office> offices;
    public List<Volunteer> volunteers;
    private final ChatTool chat;
    private final HRSystem hr;
    private final double nokToInrRate = 10.0;

    public SnackCounterEngine(ChatTool chat, HRSystem hr) {
        this.chat = chat;
        this.hr = hr;
        this.offices = new HashMap<>();
        this.offices.put("Oslo", new Office("Oslo", "NOK", 6000.0));
        this.offices.put("Bengaluru", new Office("Bengaluru", "INR", 15000.0));

        this.volunteers = new ArrayList<>(Arrays.asList(
            new Volunteer("emp_oslo_1", "Oslo"),
            new Volunteer("emp_oslo_2", "Oslo"),
            new Volunteer("emp_blr_1", "Bengaluru"),
            new Volunteer("emp_blr_2", "Bengaluru")
        ));
    }

    public void reportEmpty(String officeName) {
        Office office = offices.get(officeName);
        if (office == null || office.status != OfficeStatus.STOCKED) {
            return;
        }
        office.status = OfficeStatus.NEEDS_REFILL;
        assignNextVolunteer(office);
    }

    private void assignNextVolunteer(Office office) {
        List<Volunteer> availableVols = volunteers.stream()
            .filter(v -> v.homeOffice.equals(office.name))
            .filter(v -> !office.declinedCurrentRound.contains(v.employeeId))
            .sorted(Comparator.comparingDouble(v -> v.pendingSpend))
            .collect(Collectors.toList());

        if (availableVols.isEmpty()) {
            chat.sendMessage("ADMIN_CHANNEL", "All volunteers in " + office.name + " declined or are unavailable!");
            office.status = OfficeStatus.NEEDS_REFILL;
            office.assignedVolunteerId = null;
            office.assignedAt = null;
            return;
        }

        Volunteer bestCandidate = availableVols.get(0);
        office.status = OfficeStatus.ASSIGNED;
        office.assignedVolunteerId = bestCandidate.employeeId;
        office.assignedAt = LocalDateTime.now();

        double remainingBudget = office.monthlyBudget - office.currentSpend;
        chat.sendMessage(
            bestCandidate.employeeId,
            String.format(
                "The %s box is empty! Remaining budget: %.2f %s. Reply ACCEPT or DECLINE with reason.",
                office.name,
                remainingBudget,
                office.currency
            )
        );
    }

    public boolean commitToRefill(String employeeId, String officeName) {
        Office office = offices.get(officeName);
        if (office.status == OfficeStatus.ASSIGNED && !office.assignedVolunteerId.equals(employeeId)) {
            return false;
        }
        office.status = OfficeStatus.ASSIGNED;
        office.assignedVolunteerId = employeeId;
        office.assignedAt = LocalDateTime.now();
        return true;
    }

    public void declineRefill(String employeeId, String officeName, String reason) {
        Office office = offices.get(officeName);
        if (office.status != OfficeStatus.ASSIGNED || !office.assignedVolunteerId.equals(employeeId)) {
            return;
        }
        office.declineAuditLog.add(employeeId + " declined: " + reason);
        office.declinedCurrentRound.add(employeeId);
        assignNextVolunteer(office);
    }

    public void submitReceipt(
        String employeeId,
        String officeName,
        double amount,
        String description,
        String receiptUrl
    ) {
        Office office = offices.get(officeName);
        Volunteer vol = volunteers.stream()
            .filter(v -> v.employeeId.equals(employeeId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Volunteer not found"));

        office.currentSpend += amount;
        vol.pendingSpend += amount;

        hr.submitClaim(employeeId, amount, office.currency, description, receiptUrl);

        office.status = OfficeStatus.STOCKED;
        office.assignedVolunteerId = null;
        office.assignedAt = null;
        office.declinedCurrentRound.clear();
    }

    public void checkTimeouts() {
        for (Office office : offices.values()) {
            if (office.status == OfficeStatus.ASSIGNED && office.assignedAt != null) {
                long hoursAssigned = ChronoUnit.HOURS.between(office.assignedAt, LocalDateTime.now());
                if (hoursAssigned >= 24) {
                    String timedOutVolunteer = office.assignedVolunteerId;
                    office.declinedCurrentRound.add(timedOutVolunteer);
                    chat.sendMessage(timedOutVolunteer, "Task timed out. Reassigning to next volunteer.");
                    assignNextVolunteer(office);
                }
            }
        }
    }

    public Map<String, Double> generateMonthlyManagementReport() {
        double osloSpendNok = offices.get("Oslo").currentSpend;
        double blrSpendInr = offices.get("Bengaluru").currentSpend;
        double blrSpendNok = blrSpendInr / nokToInrRate;

        Map<String, Double> report = new HashMap<>();
        report.put("Oslo_NOK", osloSpendNok);
        report.put("Bengaluru_NOK", blrSpendNok);
        report.put("Total_Spend_NOK", osloSpendNok + blrSpendNok);
        return report;
    }
}
