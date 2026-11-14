package com.framsikt.snack;

public class Volunteer {
    public String employeeId;
    public String homeOffice;
    public double pendingSpend;

    public Volunteer(String employeeId, String homeOffice) {
        this.employeeId = employeeId;
        this.homeOffice = homeOffice;
        this.pendingSpend = 0.0;
    }
}
