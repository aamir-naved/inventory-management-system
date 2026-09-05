package com.inventory.desktop;

import java.io.IOException;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

import jakarta.servlet.ServletException;

public class DesktopStaticValve extends ValveBase {

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        String uri = request.getRequestURI();
        if (DesktopStaticResources.isApiRequest(uri)) {
            getNext().invoke(request, response);
            return;
        }

        var resource = DesktopStaticResources.load(uri, getClass().getClassLoader());
        if (resource.isEmpty()) {
            response.setStatus(404);
            response.finishResponse();
            return;
        }

        DesktopStaticResources.ResourceFile file = resource.get();
        response.setStatus(200);
        response.setContentType(file.contentType());
        response.setHeader("Cache-Control", file.cacheControl());
        response.setContentLength(file.bytes().length);
        response.getOutputStream().write(file.bytes());
        response.finishResponse();
    }
}
