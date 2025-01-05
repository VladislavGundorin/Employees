package org.example.controllers;

import org.example.dto.EmployeeDto;
import org.example.dto.NewEmployeeRequest;
import org.example.grpc.EmployeeGrpcClient;
import org.example.service.RabbitMQSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeGatewayController {

    private final EmployeeGrpcClient employeeGrpcClient;
    private final RabbitMQSender rabbitMQSender;

    @Autowired
    public EmployeeGatewayController(EmployeeGrpcClient employeeGrpcClient, RabbitMQSender rabbitMQSender) {
        this.employeeGrpcClient = employeeGrpcClient;
        this.rabbitMQSender = rabbitMQSender;
    }

    // GET запросы остаются синхронными через gRPC
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDto> getEmployeeById(@PathVariable Long id) {
        return employeeGrpcClient.getEmployeeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<EmployeeDto>> getAllEmployees() {
        List<EmployeeDto> employees = employeeGrpcClient.getAllEmployees();
        return ResponseEntity.ok(employees);
    }

    // POST, PUT и DELETE отправляют сообщения в RabbitMQ без общего класса
    @PostMapping
    public ResponseEntity<String> createEmployee(@RequestBody NewEmployeeRequest request) {
        String message = String.format("CREATE:%s,%s,%f,%s",
                request.getName(), request.getPosition(), request.getSalary(), request.getHireDate());
        rabbitMQSender.sendMessage(message);
        return ResponseEntity.accepted().body("Create employee request accepted");
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateEmployee(@PathVariable Long id, @RequestBody NewEmployeeRequest request) {
        String message = String.format("UPDATE:%d,%s,%s,%f,%s",
                id, request.getName(), request.getPosition(), request.getSalary(), request.getHireDate());
        rabbitMQSender.sendMessage(message);
        return ResponseEntity.accepted().body("Update employee request accepted");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteEmployee(@PathVariable Long id) {
        String message = String.format("DELETE:%d", id);
        rabbitMQSender.sendMessage(message);
        return ResponseEntity.accepted().body("Delete employee request accepted");
    }
}
