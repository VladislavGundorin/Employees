package org.example.controllers;

import org.example.dto.EmployeeDto;
import org.example.dto.NewEmployeeRequest;
import org.example.grpc.EmployeeGrpcClient;
import org.example.service.RabbitMQSender;
import org.example.service.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeGatewayController {
    private final EmployeeGrpcClient employeeGrpcClient;
    private final RabbitMQSender rabbitMQSender;
    private final RedisCacheService cacheService;
    private static final Logger log = LoggerFactory.getLogger(EmployeeGatewayController.class);

    public EmployeeGatewayController(EmployeeGrpcClient employeeGrpcClient,
                                     RabbitMQSender rabbitMQSender,
                                     RedisCacheService cacheService) {
        this.employeeGrpcClient = employeeGrpcClient;
        this.rabbitMQSender = rabbitMQSender;
        this.cacheService = cacheService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDto> getEmployeeById(@PathVariable Long id) {
        log.info("Getting employee with ID: {}", id);

        EmployeeDto cachedEmployee = cacheService.getCachedEmployee(id);
        if (cachedEmployee != null) {
            return ResponseEntity.ok(cachedEmployee);
        }

        return employeeGrpcClient.getEmployeeById(id)
                .map(employee -> {
                    cacheService.cacheEmployee(id, employee);
                    return ResponseEntity.ok(employee);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<EmployeeDto>> getAllEmployees() {
        log.info("Request to get all employees");

        List<EmployeeDto> cachedEmployees = cacheService.getCachedAllEmployees();
        if (!cachedEmployees.isEmpty()) {
            log.info("Returning {} employees from cache", cachedEmployees.size());
            return ResponseEntity.ok(cachedEmployees);
        }

        List<EmployeeDto> employees = employeeGrpcClient.getAllEmployees();
        if (employees.isEmpty()) {
            log.warn("No employees found from gRPC");
            return ResponseEntity.noContent().build();
        }

        cacheService.cacheAllEmployees(employees);
        log.info("Caching {} employees retrieved from gRPC", employees.size());
        return ResponseEntity.ok(employees);
    }


    @PostMapping
    public ResponseEntity<String> createEmployee(@RequestBody NewEmployeeRequest request) {
        log.info("Creating new employee: {}", request.getName());

        String message = String.format("CREATE:%s,%s,%f,%s",
                request.getName(), request.getPosition(),
                request.getSalary(), request.getHireDate());
        rabbitMQSender.sendMessage(message);
        cacheService.evictAllEmployees();
        log.info("Employee creation request sent to RabbitMQ and cache invalidated");
        return ResponseEntity.accepted().body("Create employee request accepted");
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateEmployee(@PathVariable Long id, @RequestBody NewEmployeeRequest request) {
        log.info("Updating employee with ID: {}", id);

        String message = String.format("UPDATE:%d,%s,%s,%f,%s",
                id, request.getName(), request.getPosition(),
                request.getSalary(), request.getHireDate());
        rabbitMQSender.sendMessage(message);
        cacheService.evictEmployee(id);
        log.info("Employee update request sent to RabbitMQ and cache invalidated for ID {}", id);
        return ResponseEntity.accepted().body("Update employee request accepted");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteEmployee(@PathVariable Long id) {
        log.info("Deleting employee with ID: {}", id);

        String message = String.format("DELETE:%d", id);
        rabbitMQSender.sendMessage(message);
        cacheService.evictEmployee(id);
        log.info("Employee deletion request sent to RabbitMQ and cache invalidated for ID {}", id);
        return ResponseEntity.accepted().body("Delete employee request accepted");
    }
}
