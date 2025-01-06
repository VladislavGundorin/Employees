package org.example.grpc;


import org.example.domainservice.*;
import org.example.dto.EmployeeDto;
import org.example.dto.NewEmployeeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmployeeGrpcClient {
    private static final Logger log = LoggerFactory.getLogger(EmployeeGrpcClient.class);
    private final EmployeeServiceGrpc.EmployeeServiceBlockingStub employeeStub;

    public EmployeeGrpcClient(EmployeeServiceGrpc.EmployeeServiceBlockingStub employeeStub) {
        this.employeeStub = employeeStub;
    }

    public Optional<EmployeeDto> getEmployeeById(Long id) {
        log.info("Getting employee by ID: {}", id);
        GetEmployeeRequest request = GetEmployeeRequest.newBuilder()
                .setId(id)
                .build();

        EmployeeResponse response = employeeStub.getEmployee(request);

        if (response.equals(EmployeeResponse.getDefaultInstance())) {
            log.debug("Employee not found with ID: {}", id);
            return Optional.empty();
        }

        log.debug("Successfully retrieved employee with ID: {}", id);
        return Optional.of(convertToDto(response));
    }

    public EmployeeDto createEmployee(NewEmployeeRequest request) {
        log.info("Creating new employee: {}", request.getName());
        CreateEmployeeRequest grpcRequest = CreateEmployeeRequest.newBuilder()
                .setName(request.getName())
                .setPosition(request.getPosition())
                .setSalary(request.getSalary())
                .setHireDate(request.getHireDate().toString())
                .build();

        CreateEmployeeResponse response = employeeStub.createEmployee(grpcRequest);
        log.debug("Employee created with ID: {}", response.getId());
        return getEmployeeById(response.getId()).orElseThrow();
    }

    public Optional<EmployeeDto> updateEmployee(Long id, NewEmployeeRequest request) {
        log.info("Updating employee with ID: {}", id);
        UpdateEmployeeRequest grpcRequest = UpdateEmployeeRequest.newBuilder()
                .setId(id)
                .setName(request.getName())
                .setPosition(request.getPosition())
                .setSalary(request.getSalary())
                .setHireDate(request.getHireDate().toString())
                .build();

        UpdateEmployeeResponse response = employeeStub.updateEmployee(grpcRequest);

        if (response.getSuccess()) {
            log.debug("Successfully updated employee with ID: {}", id);
            return getEmployeeById(id);
        }
        log.debug("Failed to update employee with ID: {}", id);
        return Optional.empty();
    }

    public boolean deleteEmployee(Long id) {
        log.info("Deleting employee with ID: {}", id);
        DeleteEmployeeRequest request = DeleteEmployeeRequest.newBuilder()
                .setId(id)
                .build();

        DeleteEmployeeResponse response = employeeStub.deleteEmployee(request);
        log.debug("Delete operation result for employee ID {}: {}", id, response.getSuccess());
        return response.getSuccess();
    }


    private EmployeeDto convertToDto(EmployeeResponse response) {
        log.trace("Converting employee response to DTO for ID: {}", response.getId());
        return new EmployeeDto(
                response.getId(),
                response.getName(),
                response.getPosition(),
                response.getSalary(),
                java.time.LocalDate.parse(response.getHireDate())
        );
    }
    public List<EmployeeDto> getAllEmployees() {
        log.info("Getting all employees");
        GetAllEmployeesRequest request = GetAllEmployeesRequest.newBuilder().build();
        GetAllEmployeesResponse response = employeeStub.getAllEmployees(request);

        List<EmployeeDto> employees = response.getEmployeesList().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        log.debug("Retrieved {} employees", employees.size());
        return employees;
    }
}
