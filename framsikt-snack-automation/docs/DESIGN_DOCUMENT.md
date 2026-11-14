# System Design Document: Automated Snack Counter Reimbursement
**Author:** Animesh Manik ([github.com/animegit](https://github.com/animegit))

## 1. Context & Refill Lifecycle (From "Empty" to "Reimbursed")
To eliminate empty boxes, duplicate purchases, unequal volunteer burden, and blind spending across Oslo and Bengaluru, the snack counter is automated as a **centralized state machine** integrated with the company's existing internal chat tool and external HR API.

1. **Trigger (`/snack empty [Office]`):** Any employee reports an empty box in chat. The office transitions from `STOCKED` to `NEEDS_REFILL`.
2. **Fair Assignment:** The engine filters volunteers in that office who haven't declined the current round and selects the volunteer with the **lowest pending reimbursement balance (`pendingSpend`)**.
3. **Locking or Declining:**
   - **Accept/Lock:** The office state becomes `ASSIGNED` and locks to that `employeeId`, preventing duplicate purchases.
   - **Decline with Reason:** If the volunteer is working from home or on holiday, they decline with a brief note (e.g., `"WFH today"`). The engine logs the note, adds them to `declinedCurrentRound`, and immediately assigns the next volunteer.
   - **24h Timeout:** An hourly check reassigns any task left incomplete for over 24 hours.
4. **Fulfillment & HR Claim:** The assigned volunteer buys snacks and uploads the receipt in chat. The engine updates `currentSpend` and `pendingSpend`, sends `(employeeId, amount, currency, description, receiptUrl)` to the external HR API, clears `declinedCurrentRound`, and resets the office to `STOCKED`.

## 2. Main System Components & Data Kept
| Component / Model | Responsibility | Key Data Stored |
| :--- | :--- | :--- |
| **Office** | Tracks local budget and state lock | `name`, `currency`, `monthlyBudget`, `currentSpend`, `status`, `assignedVolunteerId`, `assignedAt`, `declinedCurrentRound`, `declineAuditLog` |
| **Volunteer** | Tracks volunteer fairness queue | `employeeId`, `homeOffice`, `pendingSpend` |
| **SnackCounterEngine** | Orchestrates lifecycle, timeouts, and reports | `offices` map, `volunteers` list, `nokToInrRate` |
| **ChatTool / HRSystem** | External boundaries | Outbound chat notifications & HR reimbursement claims |

## 3. Problem Prevention
- **No Empty Boxes for Days:** Active direct assignment replaces passive group broadcasts, backed by 24-hour timeout handoffs.
- **No Duplicate Purchases:** State locking (`ASSIGNED`) acts as a mutex so two volunteers cannot commit to the same refill.
- **Fair Burden Distribution:** Sorting by `pendingSpend` ensures volunteers who just paid move to the back of the queue, while someone who declined due to WFH stays at `0` spend and is asked first next round.
- **Full Budget Visibility:** Volunteers see remaining monthly budget (`6,000 NOK` Oslo / `15,000 INR` Bengaluru) before purchasing, and management gets a unified NOK monthly report.

## 4. Trade-Offs & What We Deliberately Do NOT Build
- **No Custom Web/Mobile App:** Built strictly inside the internal chat tool where employees already work to ensure zero adoption friction.
- **No Itemized Snack Inventory:** Tracking individual snack bars creates administrative fatigue for unpaid volunteers; receipt descriptions are sufficient.
- **No Hard Budget Blocking on Reimbursement:** If a volunteer slightly exceeds the monthly budget, their claim is still sent to HR (since they spent personal money) and flagged in reporting.
- **No Custom Payroll FX Engine:** Claims are submitted in the currency of purchase (`NOK` or `INR`), letting the external HR system handle payroll currency conversion for visiting employees.
