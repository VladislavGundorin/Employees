package org.example.controllers;

import org.example.dto.EmployeeDto;
import org.example.dto.NewEmployeeRequest;
import org.example.grpc.EmployeeGrpcClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeGatewayController {

    private final EmployeeGrpcClient employeeGrpcClient;

    @Autowired
    public EmployeeGatewayController(EmployeeGrpcClient employeeGrpcClient) {
        this.employeeGrpcClient = employeeGrpcClient;
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDto> getEmployeeById(@PathVariable Long id) {
        return employeeGrpcClient.getEmployeeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<EmployeeDto> createEmployee(@RequestBody NewEmployeeRequest request) {
        EmployeeDto createdEmployee = employeeGrpcClient.createEmployee(request);
        return ResponseEntity.status(201).body(createdEmployee);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeDto> updateEmployee(@PathVariable Long id,
                                                      @RequestBody NewEmployeeRequest request) {
        return employeeGrpcClient.updateEmployee(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        boolean deleted = employeeGrpcClient.deleteEmployee(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
    @GetMapping
    public ResponseEntity<List<EmployeeDto>> getAllEmployees() {
        List<EmployeeDto> employees = employeeGrpcClient.getAllEmployees();
        return ResponseEntity.ok(employees);
    }
}
