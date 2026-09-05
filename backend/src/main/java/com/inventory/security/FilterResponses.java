package com.inventory.security;

import java.io.IOException;

import org.springframework.http.MediaType;

import jakarta.servlet.http.HttpServletResponse;

public final class FilterResponses {

    private FilterResponses() {
    }

    public static void json(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
            "{\"status\":" + status + ",\"error\":\"" + statusText(status)
                + "\",\"message\":\"" + escape(message) + "\"}"
        );
    }

    private static String statusText(int status) {
        return switch (status) {
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 400 -> "Bad Request";
            default -> "Error";
        };
    }

    private static String escape(String message) {
        if (message == null) {
            return "";
        }
        return message.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
