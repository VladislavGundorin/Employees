package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.EmployeeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class RedisCacheService {
    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);
    private static final String EMPLOYEE_CACHE_KEY_PREFIX = "employee:";
    private static final String ALL_EMPLOYEES_CACHE_KEY = "employees:all";
    private static final long CACHE_TTL_HOURS = 1L;

    private final RedisTemplate<String, EmployeeDto> employeeRedisTemplate;
    private final RedisTemplate<String, List<EmployeeDto>> employeeListRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCacheService(
            RedisTemplate<String, EmployeeDto> employeeRedisTemplate,
            RedisTemplate<String, List<EmployeeDto>> employeeListRedisTemplate,
            ObjectMapper objectMapper) {
        this.employeeRedisTemplate = employeeRedisTemplate;
        this.employeeListRedisTemplate = employeeListRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public EmployeeDto getCachedEmployee(Long employeeId) {
        String key = EMPLOYEE_CACHE_KEY_PREFIX + employeeId;
        log.debug("Попытка получить сотрудника из кэша по ключу: {}", key);

        try {
            Object cachedEmployee = employeeRedisTemplate.opsForValue().get(key);
            if (cachedEmployee != null) {
                log.info("КЭШ НАЙДЕН - Успешно получен сотрудник с ID: {} из кэша Redis", employeeId);
                log.debug("Необработанное значение из кэша: {}", cachedEmployee);
                EmployeeDto employee = objectMapper.convertValue(cachedEmployee, EmployeeDto.class);
                log.debug("Десериализованный сотрудник: {}", employee);
                return employee;
            }
            log.info("КЭШ ПРОПУЩЕН - Сотрудник с ID: {} не найден в кэше Redis", employeeId);
        } catch (Exception e) {
            log.error("Ошибка при получении сотрудника с ID: {} из кэша: {}", employeeId, e.getMessage());
            log.debug("Детали ошибки:", e);
        }
        return null;
    }

    public void cacheEmployee(Long id, EmployeeDto employee) {
        String key = EMPLOYEE_CACHE_KEY_PREFIX + id;
        log.debug("Попытка кэширования сотрудника с ключом: {}", key);

        try {
            employeeRedisTemplate.opsForValue().set(key, employee, CACHE_TTL_HOURS, TimeUnit.HOURS);
            log.info("КЭШ ОБНОВЛЕН - Успешно кэширован сотрудник с ID: {} в Redis", id);
            log.debug("Детали кэшированного сотрудника: {}", employee);
            log.debug("Время жизни кэша установлено на {} часов", CACHE_TTL_HOURS);

            log.debug("Инициация обновления кэша всех сотрудников после кэширования отдельного сотрудника");
            updateAllEmployeesCache();
            log.info("Операции кэширования завершены для сотрудника с ID: {}", id);
        } catch (Exception e) {
            log.error("Ошибка при кэшировании сотрудника с ID: {}: {}", id, e.getMessage());
            log.debug("Детали ошибки:", e);
        }
    }


    public List<EmployeeDto> getCachedAllEmployees() {
        log.debug("Попытка получить всех сотрудников из кэша");
        try {
            Object cachedEmployees = employeeListRedisTemplate.opsForValue().get(ALL_EMPLOYEES_CACHE_KEY);

            if (cachedEmployees != null) {
                List<EmployeeDto> employees = (List<EmployeeDto>) cachedEmployees;
                log.info("КЭШ НАЙДЕН - Получены все сотрудники из кэша Redis. Количество: {}", employees.size());
                return employees;
            }
        } catch (Exception e) {
            log.error("Ошибка при получении всех сотрудников из кэша", e);
        }
        log.warn("КЭШ ПРОПУЩЕН - Сотрудники не найдены в кэше Redis");
        return Collections.emptyList();
    }


    public void cacheAllEmployees(List<EmployeeDto> employees) {
        log.debug("Попытка кэширования списка всех сотрудников");
        try {
            if (employees == null || employees.isEmpty()) {
                log.warn("Невозможно кэшировать пустой или null список сотрудников");
                return;
            }

            employeeListRedisTemplate.opsForValue().set(ALL_EMPLOYEES_CACHE_KEY, employees, 1, TimeUnit.HOURS);
            log.info("КЭШ ОБНОВЛЕН - Успешно кэшировано {} сотрудников в Redis", employees.size());
            log.debug("Кэшированные сотрудники: {}", employees);
        } catch (Exception e) {
            log.error("Ошибка при кэшировании всех сотрудников", e);
        }
    }


    public void evictEmployee(Long id) {
        String key = EMPLOYEE_CACHE_KEY_PREFIX + id;
        log.debug("Попытка удаления сотрудника из кэша, ключ: {}", key);

        try {
            Boolean deleted = employeeRedisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("КЭШ УДАЛЕН - Успешно удален сотрудник с ID: {} из кэша Redis", id);
            } else {
                log.info("КЭШ ПРОПУЩЕН - Запись в кэше не найдена для сотрудника с ID: {}", id);
            }

            log.debug("Инициация удаления кэша всех сотрудников");
            evictAllEmployees();
            log.info("Операции удаления кэша завершены для сотрудника с ID: {}", id);
        } catch (Exception e) {
            log.error("Ошибка при удалении сотрудника с ID: {} из кэша: {}", id, e.getMessage());
            log.debug("Детали ошибки:", e);
        }
    }

    public void evictAllEmployees() {
        log.debug("Попытка удаления всех сотрудников из кэша");
        try {
            Boolean deleted = employeeListRedisTemplate.delete(ALL_EMPLOYEES_CACHE_KEY);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("КЭШ УДАЛЕН - Успешно удалены все сотрудники из кэша Redis");
            } else {
                log.info("КЭШ ПРОПУЩЕН - Записи в кэше не найдены для всех сотрудников");
            }
        } catch (Exception e) {
            log.error("Ошибка при удалении кэша всех сотрудников: {}", e.getMessage());
            log.debug("Детали ошибки:", e);
        }
    }
    private void updateAllEmployeesCache() {
        evictAllEmployees();
    }
}
