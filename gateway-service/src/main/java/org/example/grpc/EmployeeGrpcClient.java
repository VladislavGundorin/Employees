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
        log.info("Получение сотрудника по ID: {}", id);
        GetEmployeeRequest request = GetEmployeeRequest.newBuilder()
                .setId(id)
                .build();

        EmployeeResponse response = employeeStub.getEmployee(request);

        if (response.equals(EmployeeResponse.getDefaultInstance())) {
            log.debug("Сотрудник с ID: {} не найден", id);
            return Optional.empty();
        }

        log.debug("Успешно получен сотрудник с ID: {}", id);
        return Optional.of(convertToDto(response));
    }

    public EmployeeDto createEmployee(NewEmployeeRequest request) {
        log.info("Создание нового сотрудника: {}", request.getName());
        CreateEmployeeRequest grpcRequest = CreateEmployeeRequest.newBuilder()
                .setName(request.getName())
                .setPosition(request.getPosition())
                .setSalary(request.getSalary())
                .setHireDate(request.getHireDate().toString())
                .build();

        CreateEmployeeResponse response = employeeStub.createEmployee(grpcRequest);
        log.debug("Создан сотрудник с ID: {}", response.getId());
        return getEmployeeById(response.getId()).orElseThrow();
    }

    public Optional<EmployeeDto> updateEmployee(Long id, NewEmployeeRequest request) {
        log.info("Обновление сотрудника с ID: {}", id);
        UpdateEmployeeRequest grpcRequest = UpdateEmployeeRequest.newBuilder()
                .setId(id)
                .setName(request.getName())
                .setPosition(request.getPosition())
                .setSalary(request.getSalary())
                .setHireDate(request.getHireDate().toString())
                .build();

        UpdateEmployeeResponse response = employeeStub.updateEmployee(grpcRequest);

        if (response.getSuccess()) {
            log.debug("Успешно обновлен сотрудник с ID: {}", id);
            return getEmployeeById(id);
        }
        log.debug("Не удалось обновить сотрудника с ID: {}", id);
        return Optional.empty();
    }

    public boolean deleteEmployee(Long id) {
        log.info("Удаление сотрудника с ID: {}", id);
        DeleteEmployeeRequest request = DeleteEmployeeRequest.newBuilder()
                .setId(id)
                .build();

        DeleteEmployeeResponse response = employeeStub.deleteEmployee(request);
        log.debug("Результат операции удаления для сотрудника с ID {}: {}", id, response.getSuccess());
        return response.getSuccess();
    }


    private EmployeeDto convertToDto(EmployeeResponse response) {
        log.trace("Преобразование ответа сотрудника в DTO для ID: {}", response.getId());
        return new EmployeeDto(
                response.getId(),
                response.getName(),
                response.getPosition(),
                response.getSalary(),
                java.time.LocalDate.parse(response.getHireDate())
        );
    }
    public List<EmployeeDto> getAllEmployees() {
        log.info("Получение всех сотрудников");
        GetAllEmployeesRequest request = GetAllEmployeesRequest.newBuilder().build();
        GetAllEmployeesResponse response = employeeStub.getAllEmployees(request);

        List<EmployeeDto> employees = response.getEmployeesList().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        log.debug("Получено {} сотрудников", employees.size());
        return employees;
    }
}
