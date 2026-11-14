# Framsikt Snack Counter Automation

**Author:** Animesh Manik ([github.com/animegit](https://github.com/animegit))

A clean, state-machine-driven backend automation engine built in Java 17 to manage office snack counter refills, eliminate duplicate purchases, distribute volunteer financial burden fairly, and automate HR reimbursement claims across the **Oslo (NOK)** and **Bengaluru (INR)** offices.

---

## Architecture & Refill Lifecycle

```text
[1. Report Empty] ---> [2. Fair Assignment] ---> [3. Lock / Decline] ---> [4. Claim & Reimburse]
  (/snack empty)        (Lowest pending spend)     (Mutex state lock)      (HR API + Reset to STOCKED)
```

### Core Capabilities
1. **Concurrency & Duplicate Purchase Prevention:** Uses an office-level state lock (`STOCKED` -> `NEEDS_REFILL` -> `ASSIGNED`). Once assigned to a volunteer, other volunteers are prevented from committing to the same refill.
2. **Fair Load Balancing (`pendingSpend` Queue):** Volunteers are prioritized by the lowest pending out-of-pocket spend awaiting HR reimbursement.
3. **Hybrid Work & Decline Handoff:** Volunteers working from home or on holiday can decline with a reason (`declineRefill`). They are skipped for the current refill round without penalty and remain at the front of the queue for the next round.
4. **Automated Timeout Recovery:** Abandoned refills exceeding 24 hours automatically expire and rotate to the next eligible volunteer.
5. **Cross-Currency Budget & Reporting:** Tracks local office budgets (`6,000 NOK` for Oslo, `15,000 INR` for Bengaluru), delegates payroll currency conversion to the HR system API, and produces a unified management spend report in NOK.

---

## Project Structure

```text
framsikt-snack-automation/
├── pom.xml                                      # Maven configuration (Java 17 & JUnit 5)
├── README.md                                    # Project overview and execution guide
├── docs/
│   └── DESIGN_DOCUMENT.md                       # Complete 1-2 page System Design Document
└── src/
    ├── main/java/com/framsikt/snack/
    │   ├── ChatTool.java                        # External chat integration interface
    │   ├── HRSystem.java                        # External HR reimbursement system interface
    │   ├── MockChatTool.java                    # Simulated chat bot adapter
    │   ├── MockHRSystem.java                    # Simulated HR API client
    │   ├── OfficeStatus.java                    # State machine enum (STOCKED, NEEDS_REFILL, ASSIGNED)
    │   ├── Office.java                          # Office state, budget, and round audit model
    │   ├── Volunteer.java                       # Volunteer profile and pending spend tracker
    │   └── SnackCounterEngine.java              # Core refill lifecycle and reporting engine
    └── test/java/com/framsikt/snack/
        └── SnackCounterTest.java                # Automated JUnit 5 test suite proving all rules
```

---

## Running the Automated Tests

### Prerequisites
- **Java 17+**
- **Maven 3.8+**

### Execute Tests
```bash
mvn clean test
```

### Test Scenarios Covered (`SnackCounterTest.java`)
- `testTwoPeopleCannotCommitToSameRefill`: Verifies mutex locking prevents two volunteers from purchasing snacks for the same empty box.
- `testDeclineWithReasonAndFairRotationNextRound`: Verifies declining logs the reason, routes to the next volunteer, and keeps the declining user at `0` spend so they are selected first in the next round.
- `testIncompleteRefillIsHandedOver`: Verifies 24-hour timeout auto-reassigns abandoned tasks.
- `testBudgetsAndTotalsAcrossCurrencies`: Verifies local budget tracking and unified NOK management report calculation across Oslo and Bengaluru.
