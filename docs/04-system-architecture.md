This document is intentionally **practical**. It describes how we'll build the system, not theoretical architecture diagrams.

# Inventory Management System - System Architecture

## Purpose

This document defines the technical architecture of the product.

It serves as the reference for backend, frontend, database, deployment, and development standards.

This is a living document and will evolve with the product.

---

# Architecture Overview

```
                    Browser

                       │

                 React Frontend

                       │

               REST API (HTTPS)

                       │

            Spring Boot Backend

                       │

                  PostgreSQL

                       │

              Object Storage (Future)

                       │

             Email / Notifications
```

---

# Technology Stack

## Frontend

* React
* TypeScript
* Vite
* React Router
* TanStack Query
* Tailwind CSS
* shadcn/ui

---

## Backend

* Java 21 LTS ( Currently Java 17 is installed in my mac, we need to upgrade)
* Spring Boot 3
* Spring Security
* Spring Data JPA
* Hibernate
* Flyway
* Bean Validation

---

## Database

* PostgreSQL

Reason:

* Reliable
* ACID compliant
* Excellent indexing
* Mature ecosystem

---

## Authentication

* JWT Access Token
* Refresh Token
* BCrypt Password Encoding

Future:

OAuth Login

Google Login

---

## API Style

REST APIs

Example

```
GET     /api/products

POST    /api/products

PUT     /api/products/{id}

GET     /api/products/{id}

PATCH   /api/products/{id}

DELETE  /api/products/{id}
```

---

# High-Level Modules

## Authentication

Handles

* Registration
* Login
* Password Reset
* Security

---

## Business Module

Responsible for

* Business Information
* Settings

---

## Product Module

Responsible for

* Products
* Categories
* Units

---

## Inventory Module

Responsible for

* Stock
* Stock Adjustments
* Stock History

---

## Purchase Module

Responsible for

* Purchases
* Purchase Items

Automatically updates inventory.

---

## Sales Module

Responsible for

* Sales
* Sale Items

Automatically updates inventory.

---

## Payments Module

Responsible for

* Customer payments against sales
* Supplier payments against purchases
* Amount paid, outstanding balance, and derived payment status

Payments are append-only ledger entries nested under sales and purchases. Status is derived from amount paid vs billable amount (sale net after returns; purchase total).

---

## Customer Module

Responsible for customer management.

---

## Supplier Module

Responsible for supplier management.

---

## Dashboard Module

Provides dashboard metrics.

---

## Reports Module

Generates business reports.

---

# Backend Package Structure

```
com.inventory

├── auth
├── business
├── customer
├── supplier
├── product
├── inventory
├── purchase
├── sales
├── payment
├── dashboard
├── reports
├── common
├── security
├── config
└── exception
```

Each module contains

```
controller

service

repository

entity

dto

mapper

validator
```

---

# Frontend Structure

```
src

├── api
├── assets
├── components
├── features
│
│── authentication
│── products
│── inventory
│── purchases
│── sales
│── payments
│── customers
│── suppliers
│── dashboard
│── reports
│
├── hooks
├── layouts
├── pages
├── routes
├── services
├── store
├── types
└── utils
```

---

# Database Principles

* UUID Primary Keys
* Audit Fields
* Soft Delete
* Foreign Key Constraints
* Indexed Search Columns
* Flyway for schema migrations

Every table should include

* id
* created_at
* updated_at
* created_by
* updated_by

---

# Business Rules

## Inventory

Inventory should never be updated directly.

Only these actions may change stock:

* Purchase
* Sale
* Stock Adjustment

This guarantees a complete stock history.

---

## Deletion

Business records are never permanently deleted.

Use Archive instead.

---

## Transactions

Every Purchase

* Creates Purchase
* Creates Purchase Items
* Updates Stock

Everything succeeds or everything rolls back.

Same rule applies to Sales.

---

# Error Handling

Use standard API response format.

```
{
  "success": true,
  "message": "",
  "data": {}
}
```

Errors

```
{
  "success": false,
  "message": "Product not found",
  "errors": []
}
```

---

# Logging

Log

* Authentication Events
* Purchases
* Sales
* Stock Adjustments
* Errors

Never log

* Passwords
* Tokens
* Sensitive user data

---

# Security

* JWT Authentication
* HTTPS Only
* Password Encryption
* Input Validation
* SQL Injection Protection
* XSS Protection
* CSRF Disabled for JWT APIs

---

# Testing Strategy

Backend

* Unit Tests
* Integration Tests

Frontend

* Component Tests

Manual

* End-to-End Testing before release

---

# Deployment

## Backend

Docker Container

Deploy to AWS EC2

---

## Frontend

Vercel

---

## Database

Managed PostgreSQL

---

# Monitoring (Future)

* Health Endpoint
* Metrics
* Centralized Logs
* Error Tracking

---

# Coding Standards

## Backend

* Constructor Injection
* No Field Injection
* Layered Architecture
* DTOs for API communication
* Services contain business logic
* Controllers remain thin

---

## Frontend

* Reusable Components
* Feature-based organization
* Responsive Design
* API logic separated from UI

---

# Future Architecture

The MVP will begin as a well-structured modular monolith.

As the product grows, modules can be extracted into microservices if there is a proven scalability or operational need.

Microservices are **not** a goal—they are an option for the future.

---

# Architecture Principles

1. Keep the code simple.
2. Prefer readability over cleverness.
3. Build for maintainability.
4. Protect business data integrity.
5. Optimize only after measuring.
6. Every technical decision should support the Product Vision.

---

# Definition of Good Architecture

A new developer should be able to clone the repository, understand the project structure, run the application locally, and contribute their first feature within one day.

### One architectural recommendation

Based on your experience with **Java, Spring Boot, PostgreSQL, AWS, Terraform, and building SaaS products**, I recommend the following stack for this project:

* **Backend:** Java 21 + Spring Boot 3.x
* **Frontend:** React + TypeScript + Vite + Tailwind CSS + shadcn/ui
* **Database:** PostgreSQL + Flyway
* **Authentication:** JWT + Refresh Tokens
* **Deployment:** Docker, EC2 (backend), Vercel (frontend)
* **Storage:** S3-compatible object storage (when product images are added)

One additional architectural decision I'd make now—even before writing the first line of code—is to design the application as **multi-tenant from day one**. Even if the MVP only allows one business per account, making every business record scoped by a `business_id` from the beginning avoids painful database changes later when you onboard multiple businesses or support staff accounts. This is a small upfront investment with a large long-term payoff.
