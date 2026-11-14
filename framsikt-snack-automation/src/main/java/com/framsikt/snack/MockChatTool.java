package com.framsikt.snack;

public class MockChatTool implements ChatTool {
    @Override
    public void sendMessage(String employeeId, String message) {
        System.out.println("[Chat -> " + employeeId + "]: " + message);
    }
}
