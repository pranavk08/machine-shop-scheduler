# 🏭 Machine Shop Scheduler

### Production Scheduling & Shop-Floor Management System

A full-stack production scheduling system designed for a precision machine shop environment. The system models machines, operators, customers, orders, multi-step operations, machine capabilities, shifts, breakdowns, and production schedules through a Spring Boot backend and React frontend.

The project is designed around the **Machine Shop Scheduler — Sridhar Precision Works** technical assessment, with a focus on realistic manufacturing constraints and disruption-aware production planning.

---

## 📌 Project Overview

Sridhar Precision Works operates a multi-machine production floor where every order may require several sequential manufacturing operations.

A typical production route may look like:

```text
Turning
   ↓
Milling
   ↓
Drilling
   ↓
Grinding
   ↓
Inspection
```

Scheduling becomes difficult because:

* Different operations require different machine types.
* Operators have different skills.
* Machines have limited availability.
* Orders have different due dates and customer priorities.
* Operations must respect their sequence.
* Machine breakdowns can affect downstream operations.
* Changeovers can consume production capacity.
* Production schedules need to be recalculated when shop-floor conditions change.

This project provides a foundation for managing these constraints through a centralized production scheduling application.

---

# 🎯 Objectives

The system aims to:

1. Manage machines and machine capabilities.
2. Manage operators, skills and shifts.
3. Manage customers and customer priorities.
4. Manage production orders and operations.
5. Generate production schedules.
6. Respect machine and operator availability.
7. Track machine breakdowns.
8. Provide a supervisor-friendly production dashboard.
9. Support disruption-based rescheduling.
10. Provide production and scheduling information through a web interface.

---

# 🛠️ Technology Stack

## Backend

* Java 17
* Spring Boot
* Spring Data JPA
* Hibernate
* Maven
* MySQL
* REST APIs

## Frontend

* React
* Vite
* React Router
* Tailwind CSS
* JavaScript / JSX

## Development Tools

* Eclipse / Spring Tool Suite
* Visual Studio Code
* MySQL
* Postman
* Git
* GitHub

---

# 🏗️ System Architecture

```text
                         ┌──────────────────────┐
                         │      React UI        │
                         │                      │
                         │ Dashboard             │
                         │ Orders                │
                         │ Schedule              │
                         │ Machines              │
                         │ Operators             │
                         │ Disruptions           │
                         │ Login                 │
                         └──────────┬───────────┘
                                    │
                              REST API / HTTP
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │   Spring Boot API    │
                         │                      │
                         │ Controllers          │
                         │ Services             │
                         │ Scheduler            │
                         │ Repositories         │
                         └──────────┬───────────┘
                                    │
                                  JPA
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │        MySQL         │
                         │                      │
                         │ Machines             │
                         │ Operators            │
                         │ Orders               │
                         │ Operations           │
                         │ Customers             │
                         │ Shifts               │
                         │ Skills               │
                         │ Breakdowns            │
                         │ Changeovers           │
                         └──────────────────────┘
```

---

# 📂 Project Structure

```text
machine-shop-scheduler/
│
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   │   └── Sidebar.jsx
│   │   │
│   │   ├── page/
│   │   │   ├── Dashboard.jsx
│   │   │   ├── Orders.jsx
│   │   │   ├── Schedule.jsx
│   │   │   ├── Machines.jsx
│   │   │   ├── Operators.jsx
│   │   │   ├── Disruptions.jsx
│   │   │   └── Login.jsx
│   │   │
│   │   ├── services/
│   │   │   └── api.js
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
│   │   │
│   │   └── resources/
│   │
│   └── test/
│
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .gitignore
└── README.md
```

---

# ⚙️ Backend Architecture

The backend follows a layered Spring Boot architecture.

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Entity
    ↓
MySQL
```

The scheduling logic is separated into a dedicated scheduler package:

```text
scheduler/
├── SchedulerService.java
├── MachineAvailability.java
├── MachineBooking.java
├── OperatorBooking.java
└── ScheduleResult.java
```

This separation allows the scheduling logic to evolve independently from the REST API and database layers.

---

# 🗃️ Domain Model

The backend currently models the following major manufacturing concepts.

### Machine

Represents production equipment.

Example attributes:

```text
machineCode
name
type
available
```

### Machine Capability

Defines which operations or machine types a machine can support.

### Operator

Represents a production operator.

Example information:

```text
operatorCode
name
available
```

### Operator Skill

Represents the manufacturing skills available to an operator.

### Operator Shift

Associates operators with production shifts.

### Shift

Represents a production working period.

### Customer

Represents a customer and their business priority.

Customer information includes customer code, name and customer tier.

### Order

Represents a production order.

Orders contain information such as:

```text
order number
customer
quantity
part family
due date
status
```

### Operation

Represents an individual manufacturing step belonging to an order.

Example:

```text
Sequence: 1
Operation: TURNING
Processing Time: 90 minutes
Required Machine Type: TURNING
```

### Changeover

Represents sequence-dependent production setup/changeover information.

### Breakdown

Represents machine downtime and breakdown periods.

---

# 🧠 Scheduling Engine

The scheduling engine is implemented in:

```text
src/main/java/com/mirai/machineshop/scheduler/
```

The current scheduling implementation considers the relationship between:

```text
Orders
   ↓
Operations
   ↓
Machines
   ↓
Machine Availability
   ↓
Operators
   ↓
Operator Availability
   ↓
Production Schedule
```

The generated schedule exposes information such as:

* Order
* Operation
* Machine
* Operator
* Start time
* End time

The frontend consumes the schedule through the scheduler API.

---

# 🔌 REST API

The backend exposes REST endpoints for the main production entities.

Examples include:

```text
/api/machines
/api/operators
/api/orders
/api/breakdowns
/api/scheduler/orders/schedule
```

### Machine API

```http
GET /api/machines
GET /api/machines/{id}
POST /api/machines
DELETE /api/machines/{id}
```

### Schedule API

```http
GET /api/scheduler/orders/schedule
```

The schedule endpoint returns the generated production schedule containing the operation, machine, operator and planned start/end times.

---

# 🖥️ Frontend

The React application provides a supervisor-oriented interface.

## Dashboard

Provides a centralized overview of the production environment.

The dashboard consumes backend data for:

* Machines
* Operators
* Open orders
* Production information

---

## Orders

Displays production orders retrieved from the Spring Boot API.

The order information provides visibility into the current production workload.

---

## Production Schedule

Displays the generated production schedule.

Current schedule information includes:

```text
Order
Operation
Machine
Operator
Start
End
```

The schedule is retrieved dynamically from the backend scheduler.

---

## Machines

Provides a dedicated machine-monitoring screen.

The page is intended to show:

* Machine identification
* Machine type
* Availability
* Production status
* Machine capability information

---

## Operators

Provides operator visibility, including:

* Operator identity
* Skills
* Availability
* Shift information

---

## Disruptions

Provides a dedicated interface for production disruptions such as machine breakdowns.

This screen is intended to become the main supervisor workflow for:

```text
Report disruption
       ↓
Recalculate schedule
       ↓
Compare old vs new plan
       ↓
Show production impact
       ↓
Show cost impact
```

---

## Login

The frontend contains a login screen as the entry point for the supervisor-facing application.

Authentication/authorization is currently a UI-level feature and is not yet a complete production-grade security system.

---

# 📊 Current Implementation Status

| Capability                | Status        |
| ------------------------- | ------------- |
| React frontend            | ✅ Implemented |
| Spring Boot backend       | ✅ Implemented |
| MySQL persistence         | ✅ Implemented |
| Machine management        | ✅ Implemented |
| Machine capabilities      | ✅ Implemented |
| Operator management       | ✅ Implemented |
| Operator skills           | ✅ Implemented |
| Shift model               | ✅ Implemented |
| Customer model            | ✅ Implemented |
| Production orders         | ✅ Implemented |
| Multi-operation routing   | ✅ Implemented |
| Changeover model          | ✅ Implemented |
| Breakdown model           | ✅ Implemented |
| Basic schedule generation | ✅ Implemented |
| Machine availability      | ✅ Implemented |
| Operator availability     | ✅ Implemented |
| Dashboard                 | ✅ Implemented |
| Orders UI                 | ✅ Implemented |
| Schedule UI               | ✅ Implemented |
| Machines UI               | ✅ Implemented |
| Operators UI              | ✅ Implemented |
| Disruptions UI            | ✅ Implemented |
| Login UI                  | ✅ Implemented |
| GitHub repository         | ✅ Implemented |

---

# 🚧 Advanced Scheduling Roadmap

The project is intentionally being developed in stages.

The following capabilities are the major remaining implementation areas.

## 1. Disruption-Based Replanning

When a machine breaks down, the scheduler should automatically regenerate the affected production plan.

Example:

```text
Machine CNC-01
       ↓
8-hour breakdown
       ↓
Affected operations identified
       ↓
Alternative machines/operators evaluated
       ↓
Schedule regenerated
       ↓
Delivery impact calculated
```

---

## 2. Before vs After Schedule Comparison

The system should show exactly what changed after replanning.

Example:

```text
BEFORE

ORD-001
CNC-01
10:00 → 11:30


DISRUPTION

CNC-01 unavailable


AFTER

ORD-001
CNC-02
14:00 → 15:30
```

The UI should summarize:

```text
Operations moved
Orders affected
Orders delayed
Machines reassigned
Operators reassigned
```

---

# 💰 Disruption Cost Analysis

The assessment requires the scheduler to evaluate the financial consequences of disruptions.

The target cost model is:

```text
Total Disruption Cost
        =
Overtime Cost
+
Late Delivery Penalties
+
Wasted Changeover Cost
```

The final system should expose these values in the disruption analysis screen.

Example:

```text
Overtime Cost             ₹18,000
Late Penalty Exposure     ₹25,000
Wasted Changeovers         ₹4,500
----------------------------------
Total Impact              ₹47,500
```

---

# ⏱️ Overtime Planning

The scheduler should compare the cost of overtime against the cost of delayed delivery.

Example:

```text
Scenario A
Overtime = ₹15,000
Penalty  = ₹40,000

Decision → Use overtime
```

versus:

```text
Scenario B
Overtime = ₹20,000
Penalty  = ₹10,000

Decision → Accept delay
```

This allows production planning to become a cost-based decision rather than simply attempting to finish everything as early as possible.

---

# 🔄 Sequence-Dependent Changeovers

Production sequence affects capacity.

Example:

```text
Same part family
SHAFT → SHAFT
Changeover = 20 minutes


Different part family
SHAFT → HOUSING
Changeover = 180 minutes
```

The scheduler should use the changeover matrix when deciding operation sequence.

This prevents the system from treating every machine transition as having zero setup cost.

---

# 📦 Material Delay Handling

Future disruption scenarios should include late raw material.

Example:

```text
ORD-015
Material available: 14:00

Operations before 14:00
→ cannot start

Scheduler
→ moves other eligible work forward
→ reschedules ORD-015
→ calculates delivery impact
```

---

# ♻️ Rework Handling

Quality failures should be represented as additional production work.

Example:

```text
1000 pieces produced
        ↓
4% inspection failure
        ↓
40 pieces require rework
        ↓
Rework operation enters queue
        ↓
Machine capacity is consumed again
```

The rework should affect:

* Machine capacity
* Operator capacity
* Delivery dates
* Overtime requirements
* Cost calculations

---

# 🏆 Schedule Strategy Comparison

The final scheduling system should provide three planning strategies.

## Cheapest Schedule

Primary objective:

```text
Minimize production cost
```

Focus:

* Overtime
* Changeovers
* Machine utilization
* Penalties

---

## Most On-Time Schedule

Primary objective:

```text
Maximize on-time delivery
```

Focus:

* Due dates
* Customer tier
* Late penalties
* Critical orders

---

## Most Robust Schedule

Primary objective:

```text
Remain resilient to disruptions
```

Focus:

* Spare capacity
* Alternative machines
* Operator availability
* Critical bottlenecks
* Reduced dependency on single machines

---

# 👷 Supervisor Workflow

The system is designed around a practical shop-floor workflow.

```text
06:00 AM
   ↓
Supervisor opens Dashboard
   ↓
Checks machine availability
   ↓
Checks operator availability
   ↓
Reviews today's production schedule
   ↓
Checks orders at risk
   ↓
Machine breakdown occurs
   ↓
Supervisor reports disruption
   ↓
Scheduler recalculates
   ↓
Supervisor reviews impact
   ↓
Supervisor executes revised plan
```

The interface should prioritize clear operational information rather than technical terminology.

---

# 🧪 Example Disruption Scenario

A representative future test scenario:

> Tuesday, 11:00 AM — the grinding machine is unavailable for 8+ hours. One of the three qualified grinding operators is absent. A tier-1 customer has a just-in-time delivery scheduled for Thursday at 6:00 AM.

The scheduler should determine:

1. Which operations are affected?
2. Which machines can perform the work?
3. Which operators are available?
4. Which orders become late?
5. Can overtime prevent the delay?
6. What is the overtime cost?
7. What is the penalty exposure?
8. What changeovers are required?
9. Which schedule strategy is best?
10. What action should the owner take immediately?

---

# 🔐 Configuration & Security

Database credentials should never be committed to source control.

Local configuration:

```text
src/main/resources/application.properties
```

is intentionally excluded from Git.

A safe example configuration is provided as:

```text
src/main/resources/application-example.properties
```

Configure the local database credentials before running the backend.

---

# 🚀 Running the Project Locally

## Prerequisites

Install:

* Java 17+
* Maven or Maven Wrapper
* MySQL
* Node.js 18+
* npm
* Git

---

## 1. Clone Repository

```bash
git clone https://github.com/pranavk08/machine-shop-scheduler.git
cd machine-shop-scheduler
```

---

## 2. Configure MySQL

Create the database:

```sql
CREATE DATABASE machine_shop_scheduler;
```

Configure your local Spring Boot database credentials in:

```text
src/main/resources/application.properties
```

Example:

```properties
spring.application.name=machine-shop-scheduler

spring.datasource.url=jdbc:mysql://localhost:3306/machine_shop_scheduler
spring.datasource.username=root
spring.datasource.password=YOUR_DB_PASSWORD

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

server.port=8080
```

---

# ▶️ Start Backend

From the project root:

### Windows

```powershell
.\mvnw.cmd spring-boot:run
```

Or run:

```text
MachineShopSchedulerApplication
```

from Eclipse / Spring Tool Suite.

Backend:

```text
http://localhost:8080
```

---

# ▶️ Start Frontend

Open another terminal:

```powershell
cd frontend
npm install
npm run dev
```

The Vite development server will provide the frontend URL shown in the terminal, typically:

```text
http://localhost:5173
```

---

# 🔗 Application Flow

```text
Browser
   │
   ▼
React Frontend
   │
   │ REST API
   ▼
Spring Boot
   │
   ├── Controllers
   ├── Services
   ├── Scheduler
   └── Repositories
          │
          ▼
        MySQL
```

---

# 🧩 Assessment Requirement Mapping

| Mirai Labs Requirement        | Implementation                                                          |
| ----------------------------- | ----------------------------------------------------------------------- |
| Generate realistic shop data  | ✅ Core domain/data initialization                                       |
| Machines                      | ✅                                                                       |
| Machine capabilities          | ✅                                                                       |
| Operators and skills          | ✅                                                                       |
| Shift roster                  | ✅                                                                       |
| ~25 open orders               | ✅                                                                       |
| Multi-step routings           | ✅                                                                       |
| Changeover matrix             | ✅ Model implemented                                                     |
| Breakdown history             | ✅                                                                       |
| 2-week production scheduling  | 🟡 Core scheduling implemented; presentation/validation can be improved |
| Machine disruption replanning | 🚧 Next major implementation                                            |
| Operator absence replanning   | 🚧 Planned                                                              |
| Material delay replanning     | 🚧 Planned                                                              |
| Rework scheduling             | 🚧 Planned                                                              |
| Overtime economics            | 🚧 Planned                                                              |
| Late-delivery penalties       | 🚧 Planned                                                              |
| Wasted changeover cost        | 🚧 Planned                                                              |
| Cheapest schedule             | 🚧 Planned                                                              |
| Most on-time schedule         | 🚧 Planned                                                              |
| Most robust schedule          | 🚧 Planned                                                              |
| Supervisor dashboard          | ✅ Core UI                                                               |
| Disruption workflow           | 🟡 UI exists; advanced workflow to be completed                         |
| Trade-off memo                | 🚧 To be documented after strategy implementation                       |
| Live disruption defense       | 🚧 Final preparation                                                    |

---

# 📈 Development Roadmap

### Phase 1 — Core Platform

* [x] Spring Boot backend
* [x] React frontend
* [x] MySQL database
* [x] Production domain model
* [x] CRUD APIs
* [x] Basic scheduling
* [x] Dashboard
* [x] Production schedule
* [x] Machines
* [x] Operators
* [x] Orders
* [x] Disruptions UI

### Phase 2 — Scheduling Intelligence

* [ ] Automatic disruption replanning
* [ ] Operator absence handling
* [ ] Material delay handling
* [ ] Rework handling
* [ ] Alternative machine selection
* [ ] Changeover-aware sequencing
* [ ] Schedule impact comparison

### Phase 3 — Cost Optimization

* [ ] Overtime calculation
* [ ] Late penalty calculation
* [ ] Changeover cost calculation
* [ ] Total disruption cost
* [ ] Cheapest schedule
* [ ] Most on-time schedule
* [ ] Most robust schedule
* [ ] Strategy comparison

### Phase 4 — Production-Ready UX

* [ ] Supervisor-oriented schedule board
* [ ] Machine status visualization
* [ ] Orders-at-risk indicators
* [ ] Cost/impact dashboard
* [ ] Improved disruption workflow

### Phase 5 — Deployment

* [ ] Production database
* [ ] Environment-based configuration
* [ ] Backend deployment
* [ ] Frontend deployment
* [ ] CORS production configuration
* [ ] Production health checks
* [ ] End-to-end testing

---

# 🎓 Technical Assessment Focus

The most important part of this project is not simply displaying production data.

The core objective is to demonstrate how software can help a manufacturing supervisor answer:

> **"What should we run, on which machine, with which operator, and what should we do when reality changes?"**

The next stage of the project therefore focuses on **dynamic scheduling, disruption recovery, cost trade-offs, and operational decision support**.

---

# 📌 Current Project Status

**Core full-stack platform:** Implemented

**Scheduling foundation:** Implemented

**Supervisor UI:** Implemented

**Advanced optimization and disruption economics:** In progress

The current version is suitable for demonstrating the application's architecture, domain modeling, API integration, scheduling foundation and frontend workflow. The remaining optimization features are the primary focus for completing the full technical-assessment scope.

---

# 👨‍💻 Author

**Pranav Kamble**

Aspiring Software Developer | Full-Stack Developer

GitHub:

https://github.com/pranavk08

---

# 📄 License

This project was developed as part of a technical assessment and portfolio demonstration.
