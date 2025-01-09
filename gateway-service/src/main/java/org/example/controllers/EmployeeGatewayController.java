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
        log.info("Получение сотрудника с ID: {}", id);

        EmployeeDto cachedEmployee = cacheService.getCachedEmployee(id);
        if (cachedEmployee != null) {
            log.debug("Найдено в кэше для сотрудника с ID:  {}", id);
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
        log.info("Запрос на получение всех сотрудников");

        List<EmployeeDto> cachedEmployees = cacheService.getCachedAllEmployees();
        if (!cachedEmployees.isEmpty()) {
            log.info("Возвращаем {} сотрудников из кэша", cachedEmployees.size());
            return ResponseEntity.ok(cachedEmployees);
        }

        List<EmployeeDto> employees = employeeGrpcClient.getAllEmployees();
        if (employees.isEmpty()) {
            log.warn("Сотрудники не найдены через gRPC");
            return ResponseEntity.noContent().build();
        }

        cacheService.cacheAllEmployees(employees);
        log.info("Кэширование {} сотрудников, полученных через gRPC", employees.size());
        return ResponseEntity.ok(employees);
    }


    @PostMapping
    public ResponseEntity<String> createEmployee(@RequestBody NewEmployeeRequest request) {
        log.info("Создание нового сотрудника: {}", request.getName());

        String message = String.format("CREATE:%s,%s,%f,%s",
                request.getName(), request.getPosition(),
                request.getSalary(), request.getHireDate());
        rabbitMQSender.sendMessage(message);
        cacheService.evictAllEmployees();
        log.info("Запрос на создание сотрудника отправлен в RabbitMQ и кэш очищен");
        return ResponseEntity.accepted().body("Запрос на создание сотрудника принят");
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateEmployee(@PathVariable Long id, @RequestBody NewEmployeeRequest request) {
        log.info("Обновление сотрудника с ID: {}", id);

        String message = String.format("UPDATE:%d,%s,%s,%f,%s",
                id, request.getName(), request.getPosition(),
                request.getSalary(), request.getHireDate());
        rabbitMQSender.sendMessage(message);
        cacheService.evictEmployee(id);
        log.info("Запрос на обновление сотрудника отправлен в RabbitMQ и кэш очищен для ID {}", id);
        return ResponseEntity.accepted().body("Запрос на обновление сотрудника принят");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteEmployee(@PathVariable Long id) {
        log.info("Удаление сотрудника с ID:  {}", id);

        String message = String.format("DELETE:%d", id);
        rabbitMQSender.sendMessage(message);
        cacheService.evictEmployee(id);
        log.info("Запрос на удаление сотрудника отправлен в RabbitMQ и кэш очищен для ID {}", id);
        return ResponseEntity.accepted().body("Запрос на удаление сотрудника принят");
    }
}
