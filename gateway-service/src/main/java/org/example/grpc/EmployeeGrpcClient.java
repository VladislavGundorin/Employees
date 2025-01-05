package org.example.grpc;


import org.example.domainservice.*;
import org.example.dto.EmployeeDto;
import org.example.dto.NewEmployeeRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmployeeGrpcClient {
    private final EmployeeServiceGrpc.EmployeeServiceBlockingStub employeeStub;

    public EmployeeGrpcClient(EmployeeServiceGrpc.EmployeeServiceBlockingStub employeeStub) {
        this.employeeStub = employeeStub;
    }

    public Optional<EmployeeDto> getEmployeeById(Long id) {
        GetEmployeeRequest request = GetEmployeeRequest.newBuilder()
                .setId(id)
                .build();

        EmployeeResponse response = employeeStub.getEmployee(request);

        if (response.equals(EmployeeResponse.getDefaultInstance())) {
            return Optional.empty();
        }

        return Optional.of(convertToDto(response));
    }

    public EmployeeDto createEmployee(NewEmployeeRequest request) {
        CreateEmployeeRequest grpcRequest = CreateEmployeeRequest.newBuilder()
                .setName(request.getName())
                .setPosition(request.getPosition())
                .setSalary(request.getSalary())
                .setHireDate(request.getHireDate().toString())
                .build();

        CreateEmployeeResponse response = employeeStub.createEmployee(grpcRequest);
        return getEmployeeById(response.getId()).orElseThrow();
    }

    public Optional<EmployeeDto> updateEmployee(Long id, NewEmployeeRequest request) {
        UpdateEmployeeRequest grpcRequest = UpdateEmployeeRequest.newBuilder()
                .setId(id)
                .setName(request.getName())
                .setPosition(request.getPosition())
                .setSalary(request.getSalary())
                .setHireDate(request.getHireDate().toString())
                .build();

        UpdateEmployeeResponse response = employeeStub.updateEmployee(grpcRequest);

        if (response.getSuccess()) {
            return getEmployeeById(id);
        }
        return Optional.empty();
    }

    public boolean deleteEmployee(Long id) {
        DeleteEmployeeRequest request = DeleteEmployeeRequest.newBuilder()
                .setId(id)
                .build();

        DeleteEmployeeResponse response = employeeStub.deleteEmployee(request);
        return response.getSuccess();
    }

    private EmployeeDto convertToDto(EmployeeResponse response) {
        return new EmployeeDto(
                response.getId(),
                response.getName(),
                response.getPosition(),
                response.getSalary(),
                java.time.LocalDate.parse(response.getHireDate())
        );
    }
    public List<EmployeeDto> getAllEmployees() {
        GetAllEmployeesRequest request = GetAllEmployeesRequest.newBuilder().build();
        GetAllEmployeesResponse response = employeeStub.getAllEmployees(request);

        return response.getEmployeesList().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
}
