package com.mirai.machineshop.config;

import com.mirai.machineshop.entity.*;

import com.mirai.machineshop.entity.Breakdown;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final EntityManager entityManager;

    public DataInitializer(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void run(String... args) {

        System.out.println("======================================");
        System.out.println("Starting factory data initialization...");
        System.out.println("======================================");

        createShifts();
        createMachines();
        createOperators();
        createCustomers();

        createOperatorSkills();
        createMachineCapabilities();
        createChangeovers();
        createOperatorShifts();
        createOrders();

        System.out.println("Factory master data created successfully.");
    }
    
    
    private void createOperatorSkills() {

        if (count(OperatorSkill.class) > 0) {
            return;
        }

        addSkill("OP-001", "TURNING");
        addSkill("OP-001", "MILLING");

        addSkill("OP-002", "TURNING");

        addSkill("OP-003", "MILLING");

        addSkill("OP-004", "MILLING");
        addSkill("OP-004", "DRILLING");

        addSkill("OP-005", "DRILLING");

        // Only 3 operators have GRINDING skill
        addSkill("OP-006", "GRINDING");
        addSkill("OP-007", "GRINDING");
        addSkill("OP-008", "GRINDING");

        addSkill("OP-009", "TURNING");
        addSkill("OP-009", "DRILLING");

        addSkill("OP-010", "MILLING");

        System.out.println("Created operator skills.");
    }
    
    private void addSkill(String operatorCode, String skillName) {

        Operator operator = findOperator(operatorCode);

        if (operator != null) {
            entityManager.persist(
                    new OperatorSkill(operator, skillName)
            );
        }
    }
    
    
    private void createOrders() {

        if (count(Order.class) > 0) {
            return;
        }

        createOrder("ORD-001", "CUST-001", 1200, "SHAFT", 2,
                new String[][]{
                        {"TURNING", "90", "TURNING"},
                        {"MILLING", "60", "MILLING"},
                        {"DRILLING", "30", "DRILLING"},
                        {"GRINDING", "45", "GRINDING"}
                });

        createOrder("ORD-002", "CUST-002", 800, "GEAR", 3,
                new String[][]{
                        {"TURNING", "100", "TURNING"},
                        {"MILLING", "75", "MILLING"},
                        {"GRINDING", "50", "GRINDING"}
                });

        createOrder("ORD-003", "CUST-003", 2500, "HOUSING", 4,
                new String[][]{
                        {"MILLING", "80", "MILLING"},
                        {"DRILLING", "40", "DRILLING"},
                        {"GRINDING", "60", "GRINDING"}
                });

        createOrder("ORD-004", "CUST-004", 600, "SHAFT", 5,
                new String[][]{
                        {"TURNING", "120", "TURNING"},
                        {"MILLING", "70", "MILLING"},
                        {"DRILLING", "35", "DRILLING"},
                        {"GRINDING", "55", "GRINDING"}
                });

        createOrder("ORD-005", "CUST-005", 1800, "GEAR", 6,
                new String[][]{
                        {"TURNING", "90", "TURNING"},
                        {"MILLING", "65", "MILLING"},
                        {"DRILLING", "30", "DRILLING"}
                });

        createOrder("ORD-006", "CUST-006", 3200, "HOUSING", 7,
                new String[][]{
                        {"MILLING", "100", "MILLING"},
                        {"DRILLING", "45", "DRILLING"},
                        {"GRINDING", "70", "GRINDING"}
                });

        createOrder("ORD-007", "CUST-007", 900, "SHAFT", 8,
                new String[][]{
                        {"TURNING", "110", "TURNING"},
                        {"MILLING", "60", "MILLING"},
                        {"GRINDING", "45", "GRINDING"}
                });

        createOrder("ORD-008", "CUST-008", 1500, "GEAR", 9,
                new String[][]{
                        {"TURNING", "95", "TURNING"},
                        {"MILLING", "70", "MILLING"},
                        {"DRILLING", "35", "DRILLING"},
                        {"GRINDING", "50", "GRINDING"}
                });

        createOrder("ORD-009", "CUST-009", 2200, "HOUSING", 10,
                new String[][]{
                        {"MILLING", "90", "MILLING"},
                        {"DRILLING", "40", "DRILLING"},
                        {"GRINDING", "60", "GRINDING"}
                });

        createOrder("ORD-010", "CUST-010", 700, "SHAFT", 11,
                new String[][]{
                        {"TURNING", "100", "TURNING"},
                        {"MILLING", "65", "MILLING"},
                        {"DRILLING", "30", "DRILLING"}
                });

        createOrder("ORD-011", "CUST-011", 2800, "GEAR", 12,
                new String[][]{
                        {"TURNING", "105", "TURNING"},
                        {"MILLING", "80", "MILLING"},
                        {"GRINDING", "55", "GRINDING"}
                });

        createOrder("ORD-012", "CUST-012", 1100, "HOUSING", 13,
                new String[][]{
                        {"MILLING", "85", "MILLING"},
                        {"DRILLING", "45", "DRILLING"},
                        {"GRINDING", "65", "GRINDING"}
                });

        createOrder("ORD-013", "CUST-001", 4500, "SHAFT", 14,
                new String[][]{
                        {"TURNING", "120", "TURNING"},
                        {"MILLING", "75", "MILLING"},
                        {"DRILLING", "40", "DRILLING"},
                        {"GRINDING", "60", "GRINDING"}
                });

        createOrder("ORD-014", "CUST-002", 1300, "GEAR", 15,
                new String[][]{
                        {"TURNING", "90", "TURNING"},
                        {"MILLING", "70", "MILLING"},
                        {"DRILLING", "35", "DRILLING"}
                });

        createOrder("ORD-015", "CUST-003", 2000, "HOUSING", 16,
                new String[][]{
                        {"MILLING", "95", "MILLING"},
                        {"DRILLING", "50", "DRILLING"},
                        {"GRINDING", "70", "GRINDING"}
                });

        createOrder("ORD-016", "CUST-004", 850, "SHAFT", 17,
                new String[][]{
                        {"TURNING", "100", "TURNING"},
                        {"MILLING", "60", "MILLING"},
                        {"GRINDING", "45", "GRINDING"}
                });

        createOrder("ORD-017", "CUST-005", 3500, "GEAR", 18,
                new String[][]{
                        {"TURNING", "115", "TURNING"},
                        {"MILLING", "85", "MILLING"},
                        {"DRILLING", "40", "DRILLING"},
                        {"GRINDING", "65", "GRINDING"}
                });

        createOrder("ORD-018", "CUST-006", 1600, "HOUSING", 19,
                new String[][]{
                        {"MILLING", "90", "MILLING"},
                        {"DRILLING", "45", "DRILLING"},
                        {"GRINDING", "60", "GRINDING"}
                });

        createOrder("ORD-019", "CUST-007", 2400, "SHAFT", 20,
                new String[][]{
                        {"TURNING", "105", "TURNING"},
                        {"MILLING", "70", "MILLING"},
                        {"DRILLING", "35", "DRILLING"}
                });

        createOrder("ORD-020", "CUST-008", 950, "GEAR", 21,
                new String[][]{
                        {"TURNING", "90", "TURNING"},
                        {"MILLING", "65", "MILLING"},
                        {"GRINDING", "50", "GRINDING"}
                });

        createOrder("ORD-021", "CUST-009", 3000, "HOUSING", 22,
                new String[][]{
                        {"MILLING", "100", "MILLING"},
                        {"DRILLING", "50", "DRILLING"},
                        {"GRINDING", "75", "GRINDING"}
                });

        createOrder("ORD-022", "CUST-010", 1250, "SHAFT", 23,
                new String[][]{
                        {"TURNING", "95", "TURNING"},
                        {"MILLING", "70", "MILLING"},
                        {"DRILLING", "30", "DRILLING"},
                        {"GRINDING", "50", "GRINDING"}
                });

        createOrder("ORD-023", "CUST-011", 1900, "GEAR", 24,
                new String[][]{
                        {"TURNING", "100", "TURNING"},
                        {"MILLING", "75", "MILLING"},
                        {"GRINDING", "55", "GRINDING"}
                });

        createOrder("ORD-024", "CUST-012", 4000, "HOUSING", 25,
                new String[][]{
                        {"MILLING", "110", "MILLING"},
                        {"DRILLING", "55", "DRILLING"},
                        {"GRINDING", "80", "GRINDING"}
                });

        createOrder("ORD-025", "CUST-001", 500, "SHAFT", 26,
                new String[][]{
                        {"TURNING", "85", "TURNING"},
                        {"MILLING", "55", "MILLING"},
                        {"GRINDING", "40", "GRINDING"}
                });

        System.out.println("Created 25 orders with routings.");
    }
    
    private void createOrder(
            String orderNumber,
            String customerCode,
            int quantity,
            String partFamily,
            int dueDays,
            String[][] operations) {

        Customer customer = findCustomer(customerCode);

        LocalDateTime dueDate =
                LocalDateTime.now().plusDays(dueDays);

        Order order = new Order(
                orderNumber,
                customer,
                quantity,
                partFamily,
                dueDate,
                "OPEN"
        );

        entityManager.persist(order);

        int sequence = 1;

        for (String[] operationData : operations) {

            Operation operation = new Operation(
                    order,
                    sequence,
                    operationData[0],
                    Integer.parseInt(operationData[1]),
                    operationData[2]
            );

            entityManager.persist(operation);

            sequence++;
        }
    }
    
    private Customer findCustomer(String code) {

        return entityManager
                .createQuery(
                        "SELECT c FROM Customer c WHERE c.customerCode = :code",
                        Customer.class
                )
                .setParameter("code", code)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }
    
    private void createMachineCapabilities() {

        if (count(MachineCapability.class) > 0) {
            return;
        }

        addCapability("CNC-01", "TURNING");
        addCapability("CNC-02", "TURNING");
        addCapability("CNC-03", "TURNING");
        addCapability("CNC-04", "TURNING");
        addCapability("CNC-05", "TURNING");
        addCapability("CNC-06", "TURNING");

        addCapability("MILL-01", "MILLING");
        addCapability("MILL-02", "MILLING");
        addCapability("MILL-03", "MILLING");
        addCapability("MILL-04", "MILLING");

        addCapability("DRILL-01", "DRILLING");
        addCapability("DRILL-02", "DRILLING");
        addCapability("DRILL-03", "DRILLING");

        addCapability("GRIND-01", "GRINDING");

        System.out.println("Created machine capabilities.");
    }
    
    private void addCapability(String machineCode, String capability) {

        Machine machine = findMachine(machineCode);

        if (machine != null) {
            entityManager.persist(
                    new MachineCapability(machine, capability)
            );
        }
    }
    
    private void createChangeovers() {

        if (count(Changeover.class) > 0) {
            return;
        }

        String[] families = {
                "SHAFT",
                "GEAR",
                "HOUSING"
        };

        String[] machineCodes = {
                "CNC-01", "CNC-02", "CNC-03", "CNC-04",
                "CNC-05", "CNC-06",
                "MILL-01", "MILL-02", "MILL-03", "MILL-04",
                "DRILL-01", "DRILL-02", "DRILL-03",
                "GRIND-01"
        };

        for (String machineCode : machineCodes) {

            Machine machine = findMachine(machineCode);

            for (String from : families) {

                for (String to : families) {

                    int minutes;

                    if (from.equals(to)) {
                        minutes = 20;
                    } else {
                        minutes = 120;
                    }

                    entityManager.persist(
                            new Changeover(
                                    machine,
                                    from,
                                    to,
                                    minutes
                            )
                    );
                }
            }
        }

        System.out.println("Created changeover matrix.");
    }
    
    
    private void createOperatorShifts() {

        if (count(OperatorShift.class) > 0) {
            return;
        }

        Shift shift1 = findShift("SHIFT-1");
        Shift shift2 = findShift("SHIFT-2");

        LocalDate startDate = LocalDate.of(2026, 8, 26);

        for (int day = 0; day < 14; day++) {

            LocalDate date = startDate.plusDays(day);

            for (int i = 1; i <= 10; i++) {

                Operator operator = findOperator(
                        String.format("OP-%03d", i)
                );

                Shift shift = (i % 2 == 0) ? shift2 : shift1;

                entityManager.persist(
                        new OperatorShift(
                                operator,
                                shift,
                                date,
                                true
                        )
                );
            }
        }

        System.out.println("Created 14-day operator shift roster.");
    }
    
    
    private void createBreakdowns() {

        if (count(Breakdown.class) > 0) {
            return;
        }

        Machine machine = findMachine("GRIND-01");

        entityManager.persist(
                new Breakdown(
                        machine,
                        LocalDateTime.of(2026, 8, 28, 10, 0),
                        LocalDateTime.of(2026, 8, 28, 18, 0),
                        "Bearing failure"
                )
        );

        System.out.println("Created machine breakdown scenario.");
    }
    
    private Shift findShift(String name) {

        return entityManager
                .createQuery(
                        "SELECT s FROM Shift s WHERE s.shiftName = :name",
                        Shift.class
                )
                .setParameter("name", name)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }
    

    private void createShifts() {

        if (count(Shift.class) > 0) {
            return;
        }

        Shift shift1 = new Shift(
                "SHIFT-1",
                LocalTime.of(6, 0),
                LocalTime.of(14, 0)
        );

        Shift shift2 = new Shift(
                "SHIFT-2",
                LocalTime.of(14, 0),
                LocalTime.of(22, 0)
        );

        entityManager.persist(shift1);
        entityManager.persist(shift2);

        System.out.println("Created 2 shifts.");
    }

    private void createMachines() {

        String[][] machines = {
                {"CNC-01", "CNC Lathe 1", "TURNING"},
                {"CNC-02", "CNC Lathe 2", "TURNING"},
                {"CNC-03", "CNC Lathe 3", "TURNING"},
                {"CNC-04", "CNC Lathe 4", "TURNING"},

                {"MILL-01", "Milling Machine 1", "MILLING"},
                {"MILL-02", "Milling Machine 2", "MILLING"},
                {"MILL-03", "Milling Machine 3", "MILLING"},

                {"DRILL-01", "Drill Machine 1", "DRILLING"},
                {"DRILL-02", "Drill Machine 2", "DRILLING"},
                {"DRILL-03", "Drill Machine 3", "DRILLING"},

                {"GRIND-01", "Grinding Machine", "GRINDING"},

                {"CNC-05", "CNC Lathe 5", "TURNING"},
                {"CNC-06", "CNC Lathe 6", "TURNING"},
                {"MILL-04", "Milling Machine 4", "MILLING"}
        };

        for (String[] data : machines) {

            Machine existing = findMachine(data[0]);

            if (existing == null) {

                Machine machine = new Machine(
                        data[0],
                        data[1],
                        data[2]
                );

                entityManager.persist(machine);
            }
        }

        System.out.println("Created/verified 14 machines.");
    }

    private void createOperators() {

        String[][] operators = {
                {"OP-001", "Ravi"},
                {"OP-002", "Kumar"},
                {"OP-003", "Arun"},
                {"OP-004", "Suresh"},
                {"OP-005", "Manoj"},
                {"OP-006", "Prakash"},
                {"OP-007", "Vijay"},
                {"OP-008", "Rahul"},
                {"OP-009", "Ganesh"},
                {"OP-010", "Mahesh"}
        };

        for (String[] data : operators) {

            Operator existing = findOperator(data[0]);

            if (existing == null) {

                Operator operator = new Operator(
                        data[0],
                        data[1]
                );

                entityManager.persist(operator);
            }
        }

        System.out.println("Created/verified 10 operators.");
    }

    private void createCustomers() {

        if (count(Customer.class) > 0) {
            return;
        }

        String[][] customers = {
                {"CUST-001", "Apex Auto Systems", "TIER-1"},
                {"CUST-002", "Bharat Mobility", "TIER-1"},
                {"CUST-003", "Delta Components", "TIER-2"},
                {"CUST-004", "Prime Engineering", "TIER-2"},
                {"CUST-005", "Horizon Motors", "TIER-2"},
                {"CUST-006", "Vertex Auto Parts", "TIER-2"},
                {"CUST-007", "Nova Precision", "TIER-2"},
                {"CUST-008", "Metro Components", "TIER-2"},
                {"CUST-009", "Omega Industries", "TIER-2"},
                {"CUST-010", "Sigma Engineering", "TIER-2"},
                {"CUST-011", "Trident Auto", "TIER-2"},
                {"CUST-012", "Fusion Components", "TIER-2"}
        };

        for (String[] data : customers) {

            Customer customer = new Customer(
                    data[0],
                    data[1],
                    data[2]
            );

            entityManager.persist(customer);
        }

        System.out.println("Created 12 customers.");
    }

    private Machine findMachine(String code) {

        return entityManager
                .createQuery(
                        "SELECT m FROM Machine m WHERE m.machineCode = :code",
                        Machine.class
                )
                .setParameter("code", code)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    private Operator findOperator(String code) {

        return entityManager
                .createQuery(
                        "SELECT o FROM Operator o WHERE o.operatorCode = :code",
                        Operator.class
                )
                .setParameter("code", code)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    private long count(Class<?> entityClass) {

        return entityManager
                .createQuery(
                        "SELECT COUNT(e) FROM "
                                + entityClass.getSimpleName()
                                + " e",
                        Long.class
                )
                .getSingleResult();
    }
}