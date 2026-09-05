package com.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.inventory.desktop.DesktopRestoreOnStart;

@SpringBootApplication
public class InventoryManagementSystemApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(InventoryManagementSystemApplication.class);
        application.addListeners(new DesktopRestoreOnStart());
        application.run(args);
    }
}
