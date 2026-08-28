# 🏭 Machine Shop Scheduler

## Production Scheduling & Shop-Floor Management System

A full-stack production scheduling system for a precision machine shop environment.

The application models machines, operators, customers, orders, multi-step operations, machine capabilities, shifts, breakdowns, changeovers, scheduling strategies, disruption recovery, and production costs.

Built with Spring Boot + MySQL + React/Vite, the system is designed around the Machine Shop Scheduler technical assessment.

---

## 🎯 Project Objective

The main objective is to help a production supervisor answer:

"What should we run, on which machine, with which operator, and what should we do when reality changes?"

The current application demonstrates:

- Production order and resource management
- Constraint-aware scheduling
- Machine and operator availability
- Operator skill matching
- Shift-based scheduling
- Sequence-dependent changeovers
- Machine breakdown reporting
- Dynamic disruption replanning
- Before vs After schedule comparison
- Overtime calculation
- Late-delivery penalty calculation
- Disruption cost analysis
- Three scheduling strategies
- Strategy comparison and recommendation
- Supervisor dashboard
- Demo login and protected frontend routes

---

# 🏭 Production Flow

A typical manufacturing route can contain several sequential operations:

    Turning
       ↓
    Milling
       ↓
    Drilling
       ↓
    Grinding
       ↓
    Inspection

The scheduler must respect:

- Machine capability
- Operator skills
- Operator shifts
- Machine availability
- Operation sequence
- Customer priority
- Due dates
- Machine breakdowns
- Changeovers
- Machine conflicts
- Operator conflicts

---

# 🛠️ Technology Stack

## Backend

- Java 17
- Spring Boot
- Spring Data JPA
- Hibernate
- Maven
- MySQL
- REST APIs

## Frontend

- React
- Vite
- React Router
- Tailwind CSS
- JavaScript / JSX

## Development Tools

- Eclipse / Spring Tool Suite
- Visual Studio Code
- MySQL
- Postman
- Git
- GitHub

---

# 🏗️ Architecture

    React Frontend
          ↓
      REST APIs
          ↓
    Spring Boot
          ↓
    Controllers
          ↓
       Services
          ↓
     Repositories
          ↓
        MySQL

The backend follows a standard layered architecture:

    Controller
        ↓
      Service
        ↓
    Repository
        ↓
      Entity
        ↓
      MySQL

---

# 🧠 Scheduling Engine

The main scheduling engine is implemented in:

    src/main/java/com/mirai/machineshop/scheduler/

The scheduling flow is:

    Orders
       ↓
    Operations & sequence dependencies
       ↓
    Machine capabilities
       ↓
    Machine availability / breakdowns
       ↓
    Operator skills / shifts
       ↓
    Changeovers
       ↓
    Production Schedule

The generated schedule contains:

- Order
- Operation
- Sequence number
- Machine
- Operator
- Start time
- End time

---

# ⚙️ Scheduling Constraints

The scheduling engine considers:

- Machine capability matching
- Operator skill matching
- Operation sequence dependencies
- Machine availability
- Operator availability
- Shift coverage
- Machine breakdown windows
- Machine booking conflicts
- Operator booking conflicts
- Sequence-dependent changeovers

---

# 🏆 Three Scheduling Strategies

The application supports three planning strategies.

## 1. 🎯 MOST_ON_TIME

Prioritizes:

- Customer tier
- Earlier due dates
- Critical orders

Goal:

Maximize on-time delivery and reduce late-delivery penalties.

---

## 2. 💰 CHEAPEST_PRODUCTION

Groups compatible part families to reduce unnecessary setup and changeover time.

Goal:

- Reduce changeover impact
- Reduce overtime
- Reduce production cost
- Reduce late-delivery exposure

---

## 3. 🛡️ MOST_ROBUST

Prioritizes:

- Bottleneck operations
- Tight schedule slack
- Critical resources

Goal:

Create additional scheduling buffer and improve resilience against disruptions.

---

# ⚖️ Strategy Comparison

The application can evaluate all three strategies against the same open-order dataset.

Each strategy is evaluated using:

- Generated production schedule
- Makespan
- Overtime hours
- Overtime cost
- Late orders
- Late-delivery penalties
- Changeover cost
- Total financial cost

The system dynamically recommends the best strategy using:

    Lowest Total Cost
          ↓
    Lowest Late-Order Count
          ↓
    Lowest Makespan

The winning strategy is calculated at runtime and is not hardcoded.

---

# ⚡ Machine Breakdown & Dynamic Replanning

A supervisor can report a machine breakdown from the Disruptions page.

The workflow is:

    Report Breakdown
          ↓
    Identify affected operations
          ↓
    Preserve completed work
          ↓
    Find alternative machines/operators
          ↓
    Wait for repair if no alternative exists
          ↓
    Generate revised schedule
          ↓
    Compare Before vs After
          ↓
    Calculate financial impact

The replanning engine considers:

- Completed work preservation
- Affected operations
- Alternative capable machines
- Qualified operators
- Machine breakdown windows
- Operation dependencies
- Machine conflicts
- Operator conflicts

Example:

    CNC-01 breaks down
          ↓
    Turning operation is affected
          ↓
    CNC-02 / CNC-03 / ... are evaluated
          ↓
    Operation is reassigned
          ↓
    Revised schedule is generated

For a single-machine bottleneck such as GRIND-01, the operation waits until the breakdown ends when no alternative machine is available.

---

# 📊 Before vs After Impact Analysis

Replanning produces a comparison between the original and revised schedules.

The system tracks:

- Operations moved
- Orders delayed
- Machines reassigned
- Operators reassigned
- Start-time changes
- End-time changes
- Per-operation schedule deltas

Example:

    BEFORE

    ORD-001
    Machine: CNC-01
    Time: 10:00 → 11:30


    MACHINE BREAKDOWN

    CNC-01 unavailable


    AFTER

    ORD-001
    Machine: CNC-02
    Time: 10:00 → 11:30

---

# 💰 Overtime & Penalty Cost Analysis

The application calculates financial impact from the generated schedule.

Total disruption cost is calculated using:

    Total Cost
        =
    Overtime Cost
        +
    Late Delivery Penalties
        +
    Changeover Cost

---

## 💵 Configurable Cost Rates

The current default configuration is:

    scheduler.cost.regular-shift-capacity-minutes=480
    scheduler.cost.overtime-hourly-rate=500.0
    scheduler.cost.tier1-penalty-hourly-rate=150.0
    scheduler.cost.tier2-penalty-hourly-rate=75.0
    scheduler.cost.changeover-hourly-rate=300.0

These values are configurable through application properties.

---

## ⏱️ Overtime Calculation

For every operator and work date:

    Regular Shift Capacity = 480 minutes

    Overtime Minutes =
    max(0, Scheduled Minutes - 480)

    Overtime Cost =
    Overtime Hours × Overtime Hourly Rate

Example:

    Scheduled time = 600 minutes
    Regular capacity = 480 minutes

    Overtime = 120 minutes
              = 2 hours

    Overtime Cost = 2 × ₹500
                  = ₹1,000

---

## 🚚 Late Delivery Penalty

For every order:

    Completion Time =
    Latest operation end time

    Delay =
    max(0, Completion Time - Due Date)

Penalty is calculated based on customer tier:

    TIER-1 = ₹150 / late hour

    TIER-2 = ₹75 / late hour

The system displays:

- Late orders
- Customer tier
- Due date
- Scheduled completion
- Delay hours
- Penalty rate
- Penalty exposure

---

# 🔌 REST API

## Machines

    GET    /api/machines
    GET    /api/machines/{id}
    POST   /api/machines
    DELETE /api/machines/{id}

## Operators

    GET /api/operators

## Orders

    GET /api/orders

## Breakdowns

    GET  /api/breakdowns
    POST /api/breakdowns

Example:

    POST /api/breakdowns

Request:

    {
      "machineId": 1,
      "startTime": "2026-08-28T10:00:00",
      "endTime": "2026-08-28T18:00:00",
      "reason": "Bearing failure on spindle"
    }

## Scheduling

    GET /api/scheduler/orders/schedule

    GET /api/scheduler/orders/schedule?strategy=MOST_ON_TIME

    GET /api/scheduler/orders/schedule?strategy=CHEAPEST_PRODUCTION

    GET /api/scheduler/orders/schedule?strategy=MOST_ROBUST

    GET /api/scheduler/strategies/compare

    POST /api/scheduler/replan

The strategy parameter is optional, so the existing scheduling endpoint remains backward compatible.

---

# 🖥️ Frontend

## 🔐 Demo Login

The application includes a demo supervisor login.

    Username: admin
    Password: admin123

The authentication is intentionally client-side demo authentication using browser localStorage.

Protected routes include:

    /dashboard
    /orders
    /schedule
    /machines
    /operators
    /disruptions

This authentication is intended only for demonstration purposes and is not production-grade security.

---

# 📊 Dashboard

The Dashboard provides a supervisor-oriented production overview.

It displays:

- Open orders
- Machines
- Operators
- On-time rate
- Production status
- Machine status

---

# 📦 Orders

The Orders page displays open production orders including:

- Order number
- Customer
- Part family
- Quantity
- Due date
- Status

---

# 📅 Production Schedule

The Schedule page provides:

- Generated production schedule
- Scheduling strategy selector
- Machine assignment
- Operator assignment
- Start and end times
- Strategy comparison
- Recommended strategy

---

# 🏭 Machines

The Machines page displays:

- Machine code
- Machine name
- Machine type
- Availability
- Workshop machine inventory

---

# 👷 Operators

The Operators page displays:

- Operator code
- Operator name
- Availability
- Operator workforce information

---

# ⚡ Disruptions & Cost Analysis

The Disruptions page provides:

- Machine breakdown history
- Report Breakdown
- Replan Schedule
- Before vs After comparison
- Operations shifted
- Machines reassigned
- Operators reassigned
- Orders delayed
- Overtime cost
- Late-delivery penalty exposure
- Total disruption cost
- Operator overtime breakdown
- Late-order penalty breakdown

---

# 📂 Project Structure

    machine-shop-scheduler/
    │
    ├── frontend/
    │   ├── src/
    │   │   ├── components/
    │   │   │   ├── Sidebar.jsx
    │   │   │   └── ProtectedRoute.jsx
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
    │   │   │   ├── controller/
    │   │   │   ├── dto/
    │   │   │   ├── entity/
    │   │   │   ├── exception/
    │   │   │   ├── repository/
    │   │   │   ├── scheduler/
    │   │   │   └── service/
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

---

# 🧪 Verification

## Backend Tests

The implemented feature set has been verified with:

    41 tests
    0 failures
    0 errors
    0 skipped

    BUILD SUCCESS

The test suite covers:

- Scheduling strategies
- Cost calculations
- Breakdown reporting
- Dynamic replanning
- Date windows
- Operator availability
- API validation
- Duplicate-code validation
- Strategy comparison

---

## Frontend Build

The production frontend build was verified using:

    npm run build

Vite completed the production build successfully with no compilation errors.

---

## Git Verification

The project was also checked using:

    git diff --check

Formatting checks passed successfully.

---

# 📋 Assessment Requirement Mapping

| Requirement | Status |
|-------------|--------|
| Realistic shop data | ✅ Implemented |
| Machines | ✅ Implemented |
| Machine capabilities | ✅ Implemented |
| Operators and skills | ✅ Implemented |
| Shift roster | ✅ Implemented |
| Open production orders | ✅ Implemented |
| Multi-step routings | ✅ Implemented |
| Changeover matrix | ✅ Implemented |
| Breakdown history | ✅ Implemented |
| Machine breakdown reporting | ✅ Implemented |
| Dynamic disruption replanning | ✅ Implemented |
| Before vs After comparison | ✅ Implemented |
| Overtime economics | ✅ Implemented |
| Late-delivery penalties | ✅ Implemented |
| Changeover cost | ✅ Implemented |
| Most On-Time strategy | ✅ Implemented |
| Cheapest Production strategy | ✅ Implemented |
| Most Robust strategy | ✅ Implemented |
| Strategy comparison | ✅ Implemented |
| Dynamic strategy recommendation | ✅ Implemented |
| Supervisor dashboard | ✅ Implemented |
| Disruption workflow | ✅ Implemented |
| Demo authentication | ✅ Implemented |
| Protected frontend routes | ✅ Implemented |
| Operator absence replanning | 🚧 Planned |
| Material delay handling | 🚧 Planned |
| Rework scheduling | 🚧 Planned |
| Explicit two-week planning UI | 🟡 Future Enhancement |
| Production-grade authentication | 🚧 Future |
| Production deployment hardening | 🚧 Future |

---

# 🚧 Remaining Roadmap

The core scheduling, disruption, cost-analysis and strategy-comparison workflow has been implemented.

The following scenarios remain as future extensions:

## 1. Operator Absence Replanning

    Operator becomes unavailable
            ↓
    Find qualified replacement
            ↓
    Replan affected operations
            ↓
    Calculate delivery and cost impact

## 2. Material Delay Handling

    Material becomes unavailable
            ↓
    Identify dependent operations
            ↓
    Delay affected operations
            ↓
    Schedule other eligible work
            ↓
    Recalculate impact

## 3. Rework Scheduling

    Quality failure
            ↓
    Create rework operation
            ↓
    Allocate machine/operator
            ↓
    Update schedule
            ↓
    Recalculate delivery and cost impact

## 4. Two-Week Planning View

The scheduling engine supports future-date scheduling, but an explicit supervisor-facing rolling two-week planning interface can be added as a future UX enhancement.

## 5. Production Hardening

Future production-grade improvements include:

- Backend authentication and authorization
- Persistent schedule snapshots
- Production deployment
- Production configuration
- Health checks
- End-to-end testing
- Security hardening

---

# 👷 Supervisor Workflow

The intended supervisor workflow is:

    Login
      ↓
    Open Dashboard
      ↓
    Check Machines
      ↓
    Check Operators
      ↓
    Review Orders
      ↓
    Review Production Schedule
      ↓
    Select Scheduling Strategy
      ↓
    Machine Breakdown Occurs
      ↓
    Report Breakdown
      ↓
    Replan Schedule
      ↓
    Review Before vs After
      ↓
    Review Overtime / Penalty / Cost
      ↓
    Compare Strategies
      ↓
    Select Recommended Production Plan

---

# 🚀 Running the Project Locally

## Prerequisites

Install:

- Java 17+
- MySQL
- Node.js 18+
- npm
- Git

---

## 1. Clone Repository

    git clone https://github.com/pranavk08/machine-shop-scheduler.git

    cd machine-shop-scheduler

---

## 2. Configure MySQL

Create the database:

    CREATE DATABASE machine_shop_scheduler;

Configure local credentials in:

    src/main/resources/application.properties

Example:

    spring.application.name=machine-shop-scheduler

    spring.datasource.url=jdbc:mysql://localhost:3306/machine_shop_scheduler
    spring.datasource.username=root
    spring.datasource.password=YOUR_DB_PASSWORD

    spring.jpa.hibernate.ddl-auto=update
    spring.jpa.show-sql=true
    spring.jpa.properties.hibernate.format_sql=true

    server.port=8080

Do not commit real database credentials to GitHub.

---

## 3. Start Backend

### Windows PowerShell

From the project root:

    .\mvnw.cmd spring-boot:run

Backend runs at:

    http://localhost:8080

Alternatively, the backend can be started from Eclipse / Spring Tool Suite by running:

    MachineShopSchedulerApplication

---

## 4. Start Frontend

Open another terminal:

    cd frontend

    npm install

    npm run dev

Vite will provide a local URL similar to:

    http://localhost:5173

---

# 🔐 Security Notes

Database credentials should remain in:

    src/main/resources/application.properties

The example configuration is provided through:

    src/main/resources/application-example.properties

The frontend demo login is intentionally designed for demonstration purposes.

It is not a production security mechanism.

A production deployment should use proper authentication and authorization such as:

- Backend authentication
- Password hashing
- Session/JWT management
- Role-based access control
- Secure secrets management

---

# 📈 Current Project Status

Core full-stack platform: ✅ COMPLETE

Scheduling engine: ✅ COMPLETE

Machine/operator constraints: ✅ COMPLETE

Machine breakdown reporting: ✅ COMPLETE

Dynamic disruption replanning: ✅ COMPLETE

Before vs After impact analysis: ✅ COMPLETE

Overtime analysis: ✅ COMPLETE

Late-delivery penalty analysis: ✅ COMPLETE

Disruption cost analysis: ✅ COMPLETE

Three scheduling strategies: ✅ COMPLETE

Strategy comparison: ✅ COMPLETE

Dynamic strategy recommendation: ✅ COMPLETE

Demo authentication: ✅ COMPLETE

Protected frontend routes: ✅ COMPLETE

Remaining assessment scenarios:

- Operator absence
- Material delay
- Rework

These are planned future extensions and are not currently claimed as implemented.

---

# 👨‍💻 Author

Pranav Kamble

Aspiring Software Developer | Full-Stack Developer

GitHub:

https://github.com/pranavk08

---

# 📄 License

This project is developed for technical assessment and demonstration purposes.
