package org.example.listener;

import com.rabbitmq.client.Channel;
import org.example.models.Employee;
import org.example.service.EmployeeService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class RabbitMQListener {

    private final EmployeeService employeeService;

    public RabbitMQListener(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    // Вариант 1: Базовый вариант без задержки

    @RabbitListener(queues = "employeeQueue")
    public void handleMessage(String message) {
        System.out.println("Received message: " + message);
        processMessage(message);
    }


    /*
    // Вариант 2: С задержкой в 10 секунд
    @RabbitListener(queues = "employeeQueue")
    public void handleMessage(String message) throws InterruptedException {
        System.out.println("Received message: " + message);

        // Добавляем задержку в 20 секунд перед обработкой сообщения
        Thread.sleep(10000);

        processMessage(message);
    }
    */


    private void processMessage(String message) {
        String[] parts = message.split(":");
        String operation = parts[0];

        switch (operation) {
            case "CREATE":
                String[] createParts = parts[1].split(",");
                Employee newEmployee = new Employee();
                newEmployee.setName(createParts[0]);
                newEmployee.setPosition(createParts[1]);
                newEmployee.setSalary(Double.parseDouble(createParts[2]));
                newEmployee.setHireDate(LocalDate.parse(createParts[3]));
                employeeService.saveEmployee(newEmployee);
                break;

            case "UPDATE":
                String[] updateInfo = parts[1].split(",");
                Long id = Long.parseLong(updateInfo[0]);
                Employee updateEmployee = new Employee();
                updateEmployee.setId(id);
                updateEmployee.setName(updateInfo[1]);
                updateEmployee.setPosition(updateInfo[2]);
                updateEmployee.setSalary(Double.parseDouble(updateInfo[3]));
                updateEmployee.setHireDate(LocalDate.parse(updateInfo[4]));
                employeeService.saveEmployee(updateEmployee);
                break;

            case "DELETE":
                employeeService.deleteEmployee(Long.parseLong(parts[1]));
                break;
        }
    }
}
