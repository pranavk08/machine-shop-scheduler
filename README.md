# 🏭 Machine Shop Scheduler

A full-stack production scheduling and shop-floor management system built with **Spring Boot, MySQL, React and Vite**.

The system helps production supervisors plan orders, allocate machines and operators, handle machine breakdowns, compare scheduling strategies, and understand production cost impact.

## 🚀 Key Features

- 📅 Production Scheduling — Machine capabilities, operator skills and shifts, operation sequences, resource conflicts, and changeovers.
- ⚡ Dynamic Replanning — Report machine breakdowns, preserve completed work, reassign operations to alternative resources, and generate revised schedules.
- 📊 Before vs After Analysis — Track operations moved, machines/operators reassigned, order delays, and schedule changes.
- 💰 Cost Analysis — Calculate overtime, late-delivery penalties, changeover costs, and total disruption cost.
- 🏆 Three Scheduling Strategies:
  - MOST_ON_TIME — prioritizes customer priority and due dates.
  - CHEAPEST_PRODUCTION — reduces changeovers and production cost.
  - MOST_ROBUST — prioritizes bottlenecks and tight-slack orders.
- ⚖️ Strategy Comparison — Evaluates all three strategies and dynamically recommends the best option based on cost, late orders, and makespan.
- 🔐 Demo Login — Simple client-side authentication for demonstration purposes.

## 🔐 Demo Login

Username: `admin`  
Password: `admin123`

> Demo authentication is for demonstration purposes only and is not intended for production security.

## 🛠️ Technology Stack

Backend:
- Java 17
- Spring Boot
- Spring Data JPA / Hibernate
- MySQL
- Maven
- REST APIs

Frontend:
- React
- Vite
- React Router
- Tailwind CSS
- JavaScript / JSX

## 🖥️ Main Pages

Dashboard · Orders · Schedule · Machines · Operators · Disruptions · Login

## 🔌 Key APIs

GET  /api/orders  
GET  /api/machines  
GET  /api/operators

GET  /api/breakdowns  
POST /api/breakdowns

GET  /api/scheduler/orders/schedule  
GET  /api/scheduler/orders/schedule?strategy=MOST_ON_TIME  
GET  /api/scheduler/orders/schedule?strategy=CHEAPEST_PRODUCTION  
GET  /api/scheduler/orders/schedule?strategy=MOST_ROBUST

GET  /api/scheduler/strategies/compare  
POST /api/scheduler/replan


## 📦 Installation & Setup

### Prerequisites

Make sure the following are installed:

- Java 17+
- MySQL 8+
- Node.js 18+
- npm
- Git

### 1. Clone the Repository

    git clone https://github.com/pranavk08/machine-shop-scheduler.git
    cd machine-shop-scheduler

### 2. Configure MySQL

Create the database:

    CREATE DATABASE machine_shop_scheduler;

Update the database credentials in:

    src/main/resources/application.properties

Configure your MySQL username and password before starting the backend.

### 3. Start the Backend

From the project root:

    .\mvnw.cmd spring-boot:run

The backend will run at:

    http://localhost:8080

### 4. Start the Frontend

Open a new terminal:

    cd frontend
    npm install
    npm run dev

The frontend will run at:

    http://localhost:5173

### 5. Login

Open the frontend in your browser and use the demo credentials:

    Username: admin
    Password: admin123


## 📁 Project Structure

    machine-shop-scheduler/
    │
    ├── frontend/
    │   ├── src/
    │   │   ├── components/
    │   │   │   ├── ProtectedRoute.jsx
    │   │   │   └── Sidebar.jsx
    │   │   │
    │   │   ├── page/
    │   │   │   ├── Login.jsx
    │   │   │   ├── Dashboard.jsx
    │   │   │   ├── Orders.jsx
    │   │   │   ├── Schedule.jsx
    │   │   │   ├── Machines.jsx
    │   │   │   ├── Operators.jsx
    │   │   │   └── Disruptions.jsx
    │   │   │
    │   │   ├── App.jsx
    │   │   └── index.css
    │   │
    │   ├── package.json
    │   └── vite.config.js
    │
    ├── src/
    │   ├── main/
    │   │   ├── java/com/mirai/machineshop/
    │   │   │   ├── config/
    │   │   │   ├── controller/
    │   │   │   ├── dto/
    │   │   │   ├── entity/
    │   │   │   ├── exception/
    │   │   │   ├── repository/
    │   │   │   ├── scheduler/
    │   │   │   └── service/
    │   │   │
    │   │   └── resources/
    │   │       ├── application.properties
    │   │       └── application-example.properties
    │   │
    │   └── test/
    │       └── java/com/mirai/machineshop/
    │
    ├── pom.xml
    ├── mvnw
    ├── mvnw.cmd
    ├── .gitignore
    └── README.md

## 🧪 Verification

Backend Tests: 41  
Failures: 0  
Errors: 0  
Skipped: 0

Frontend Production Build: Successful

Git diff --check: Passed

## 📋 Project Status

### Implemented

- ✅ Constraint-aware production scheduling
- ✅ Machine and operator availability
- ✅ Operator skills and shift constraints
- ✅ Operation sequence dependencies
- ✅ Changeover handling
- ✅ Machine breakdown reporting
- ✅ Dynamic disruption replanning
- ✅ Before vs After impact analysis
- ✅ Overtime analysis
- ✅ Late-delivery penalty analysis
- ✅ Disruption cost analysis
- ✅ Three scheduling strategies
- ✅ Strategy comparison and recommendation
- ✅ Demo authentication
- ✅ Protected frontend routes

### Future Extensions

- 🚧 Operator absence replanning
- 🚧 Material delay handling
- 🚧 Rework scheduling
- 🟡 Dedicated two-week planning UI
- 🚧 Production-grade authentication

## 🚀 Running Locally

### Backend

From the project root:

    .\mvnw.cmd spring-boot:run

Backend: http://localhost:8080

### Frontend

Open another terminal:

    cd frontend
    npm install
    npm run dev

Frontend: http://localhost:5173

## 👨‍💻 Author

Pranav Kamble

GitHub: https://github.com/pranavk08/machine-shop-scheduler

---

Developed for technical assessment and demonstration purposes.
