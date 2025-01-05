package org.example.grpc;

import io.grpc.stub.StreamObserver;
import org.example.domainservice.*;
import org.example.models.Employee;
import org.example.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class EmployeeServiceGrpcImpl extends EmployeeServiceGrpc.EmployeeServiceImplBase {

    private final EmployeeRepository employeeRepository;

    @Autowired
    public EmployeeServiceGrpcImpl(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public void getEmployee(GetEmployeeRequest request, StreamObserver<EmployeeResponse> responseObserver) {
        Optional<Employee> optionalEmployee = employeeRepository.findById(request.getId());

        if (optionalEmployee.isPresent()) {
            Employee employee = optionalEmployee.get();
            EmployeeResponse response = EmployeeResponse.newBuilder()
                    .setId(employee.getId())
                    .setName(employee.getName())
                    .setPosition(employee.getPosition())
                    .setSalary(employee.getSalary())
                    .setHireDate(employee.getHireDate().toString())
                    .build();

            responseObserver.onNext(response);
        } else {
            responseObserver.onNext(EmployeeResponse.getDefaultInstance());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void createEmployee(CreateEmployeeRequest request, StreamObserver<CreateEmployeeResponse> responseObserver) {
        Employee employee = new Employee(
                request.getName(),
                request.getPosition(),
                request.getSalary(),
                LocalDate.parse(request.getHireDate())
        );

        employee = employeeRepository.save(employee);

        CreateEmployeeResponse response = CreateEmployeeResponse.newBuilder()
                .setId(employee.getId())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void updateEmployee(UpdateEmployeeRequest request, StreamObserver<UpdateEmployeeResponse> responseObserver) {
        Optional<Employee> optionalEmployee = employeeRepository.findById(request.getId());

        if (optionalEmployee.isPresent()) {
            Employee employee = optionalEmployee.get();
            employee.setName(request.getName());
            employee.setPosition(request.getPosition());
            employee.setSalary(request.getSalary());
            employee.setHireDate(LocalDate.parse(request.getHireDate()));

            employeeRepository.save(employee);

            UpdateEmployeeResponse response = UpdateEmployeeResponse.newBuilder()
                    .setSuccess(true)
                    .build();

            responseObserver.onNext(response);
        } else {
            UpdateEmployeeResponse response = UpdateEmployeeResponse.newBuilder()
                    .setSuccess(false)
                    .build();

            responseObserver.onNext(response);
        }
        responseObserver.onCompleted();
    }

    @Override
    public void deleteEmployee(DeleteEmployeeRequest request, StreamObserver<DeleteEmployeeResponse> responseObserver) {
        if (employeeRepository.existsById(request.getId())) {
            employeeRepository.deleteById(request.getId());

            DeleteEmployeeResponse response = DeleteEmployeeResponse.newBuilder()
                    .setSuccess(true)
                    .build();

            responseObserver.onNext(response);
        } else {
            DeleteEmployeeResponse response = DeleteEmployeeResponse.newBuilder()
                    .setSuccess(false)
                    .build();

            responseObserver.onNext(response);
        }
        responseObserver.onCompleted();
    }
    @Override
    public void getAllEmployees(GetAllEmployeesRequest request, StreamObserver<GetAllEmployeesResponse> responseObserver) {
        List<Employee> employees = employeeRepository.findAll();

        List<EmployeeResponse> employeeResponses = new ArrayList<>();
        for (Employee employee : employees) {
            EmployeeResponse employeeResponse = EmployeeResponse.newBuilder()
                    .setId(employee.getId())
                    .setName(employee.getName())
                    .setPosition(employee.getPosition())
                    .setSalary(employee.getSalary())
                    .setHireDate(employee.getHireDate().toString())
                    .build();
            employeeResponses.add(employeeResponse);
        }

        GetAllEmployeesResponse response = GetAllEmployeesResponse.newBuilder()
                .addAllEmployees(employeeResponses)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
