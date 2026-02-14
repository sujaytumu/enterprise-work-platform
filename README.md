# Enterprise Task Management System

A full-stack enterprise-grade Task Management platform built using **Java (Spring Boot)**, **React**, and **PostgreSQL**.  
Designed to simulate real-world internal work management systems used in large organizations.

---

## 🚀 Overview

This system enables teams to manage projects, tasks, and workflows with secure role-based access control.  
It follows clean architecture principles and production-ready backend practices.

The platform is designed as a foundation for enterprise-level workflow systems.

---

## 🏗️ Tech Stack

### Backend
- Java 17
- Spring Boot
- Spring Security (JWT Authentication)
- Spring Data JPA (Hibernate)
- PostgreSQL
- Bean Validation
- Global Exception Handling

### Frontend
- React
- REST API Integration
- Role-Based UI Rendering

### Database
- PostgreSQL (Relational)
- Normalized Schema Design
- Indexed Fields for Performance

---

## 🔐 Features

### Authentication & Authorization
- JWT-based authentication
- Role-based access control (Admin / Manager / User)
- Secure REST endpoints

### Project Management
- Create and manage projects
- Assign users to projects
- Track project-level tasks

### Task Management
- Create tasks (Bug / Feature / Task)
- Assign priority and status
- Update task lifecycle
- Track task ownership

### Dashboard
- View open vs completed tasks
- Filter by project and status
- Basic performance insights

### API Architecture
- Clean layered architecture:
  - Controller
  - Service
  - Repository
  - DTO
- Centralized exception handling
- Input validation

---

## 📊 Database Design (High-Level)

Core Entities:
- User
- Role
- Project
- Task
- Status

Relational mapping handled via JPA/Hibernate.

---

## 🧱 Architecture

Backend follows clean separation of concerns:

controller → service → repository → database

Frontend communicates via REST APIs.


---

## 🛠️ How to Run Locally


```bash

### 1️⃣ Clone the Repository
git clone https://github.com/sujaytumu/enterprise-work-platform


2️⃣ Backend Setup

Update application.properties:

spring.datasource.url=jdbc:postgresql://localhost:5432/taskdb
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update


Run backend:

mvn spring-boot:run


Backend runs at:

http://localhost:8080

3️⃣ Frontend Setup
cd client
npm install
npm start


Frontend runs at:

http://localhost:3000


