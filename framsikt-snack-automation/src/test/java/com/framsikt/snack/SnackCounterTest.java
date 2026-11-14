package com.framsikt.snack;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SnackCounterTest {

    private SnackCounterEngine engine;
    private ChatTool mockChat;
    private HRSystem mockHr;

    @BeforeEach
    public void setUp() {
        mockChat = new MockChatTool();
        mockHr = new MockHRSystem();
        engine = new SnackCounterEngine(mockChat, mockHr);
    }

    @Test
    public void testTwoPeopleCannotCommitToSameRefill() {
        engine.reportEmpty("Oslo");
        Office osloOffice = engine.offices.get("Oslo");

        assertEquals(OfficeStatus.ASSIGNED, osloOffice.status);
        assertEquals("emp_oslo_1", osloOffice.assignedVolunteerId);

        boolean canCommit = engine.commitToRefill("emp_oslo_2", "Oslo");
        assertFalse(canCommit, "System should prevent a second user from committing to a locked office");
    }

    @Test
    public void testDeclineWithReasonAndFairRotationNextRound() {
        engine.reportEmpty("Bengaluru");
        Office blr = engine.offices.get("Bengaluru");
        assertEquals("emp_blr_1", blr.assignedVolunteerId);

        engine.declineRefill("emp_blr_1", "Bengaluru", "Working from home today");
        assertEquals("emp_blr_2", blr.assignedVolunteerId);
        assertTrue(blr.declineAuditLog.get(0).contains("Working from home today"));

        engine.submitReceipt("emp_blr_2", "Bengaluru", 1500.0, "Biscuits", "receipt.png");
        assertEquals(OfficeStatus.STOCKED, blr.status);

        engine.reportEmpty("Bengaluru");
        assertEquals(
            "emp_blr_1",
            blr.assignedVolunteerId,
            "User who declined last time still has 0 spend, so they must be asked first next round!"
        );
    }

    @Test
    public void testIncompleteRefillIsHandedOver() {
        engine.reportEmpty("Oslo");
        Office osloOffice = engine.offices.get("Oslo");
        String initialAssignee = osloOffice.assignedVolunteerId;

        osloOffice.assignedAt = LocalDateTime.now().minusHours(25);

        engine.checkTimeouts();

        assertEquals(OfficeStatus.ASSIGNED, osloOffice.status);
        assertNotEquals(initialAssignee, osloOffice.assignedVolunteerId, "Task should have been handed over");
        assertEquals("emp_oslo_2", osloOffice.assignedVolunteerId);
    }

    @Test
    public void testBudgetsAndTotalsAcrossCurrencies() {
        engine.reportEmpty("Oslo");
        engine.submitReceipt("emp_oslo_1", "Oslo", 1000.0, "Chips and Soda", "receipt_url_1");

        engine.reportEmpty("Bengaluru");
        engine.submitReceipt("emp_blr_1", "Bengaluru", 2000.0, "Nuts and Tea", "receipt_url_2");

        Map<String, Double> report = engine.generateMonthlyManagementReport();

        assertEquals(1000.0, report.get("Oslo_NOK"));
        assertEquals(200.0, report.get("Bengaluru_NOK"));
        assertEquals(1200.0, report.get("Total_Spend_NOK"));
    }
}
