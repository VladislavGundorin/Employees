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
        log.debug("Attempting to get employee from cache with key: {}", key);

        try {
            Object cachedEmployee = employeeRedisTemplate.opsForValue().get(key);
            if (cachedEmployee != null) {
                log.info("CACHE HIT - Successfully retrieved employee ID: {} from Redis cache", employeeId);
                log.debug("Raw cached value: {}", cachedEmployee);
                EmployeeDto employee = objectMapper.convertValue(cachedEmployee, EmployeeDto.class);
                log.debug("Deserialized employee: {}", employee);
                return employee;
            }
            log.info("CACHE MISS - Employee ID: {} not found in Redis cache", employeeId);
        } catch (Exception e) {
            log.error("Error fetching employee ID: {} from cache: {}", employeeId, e.getMessage());
            log.debug("Error details:", e);
        }
        return null;
    }

    public void cacheEmployee(Long id, EmployeeDto employee) {
        String key = EMPLOYEE_CACHE_KEY_PREFIX + id;
        log.debug("Attempting to cache employee with key: {}", key);

        try {
            employeeRedisTemplate.opsForValue().set(key, employee, CACHE_TTL_HOURS, TimeUnit.HOURS);
            log.info("CACHE UPDATE - Successfully cached employee ID: {} in Redis", id);
            log.debug("Cached employee details: {}", employee);
            log.debug("Cache TTL set to {} hours", CACHE_TTL_HOURS);

            log.debug("Initiating update of all employees cache after individual employee cache");
            updateAllEmployeesCache();
            log.info("Cache operations completed for employee ID: {}", id);
        } catch (Exception e) {
            log.error("Error while caching employee ID: {}: {}", id, e.getMessage());
            log.debug("Error details:", e);
        }
    }


    public List<EmployeeDto> getCachedAllEmployees() {
        log.debug("Attempting to get all employees from cache");
        try {
            Object cachedEmployees = employeeListRedisTemplate.opsForValue().get(ALL_EMPLOYEES_CACHE_KEY);

            if (cachedEmployees != null) {
                List<EmployeeDto> employees = (List<EmployeeDto>) cachedEmployees;
                log.info("CACHE HIT - Retrieved all employees from Redis cache. Count: {}", employees.size());
                return employees;
            }
        } catch (Exception e) {
            log.error("Error fetching all employees from cache", e);
        }
        log.warn("CACHE MISS - No employees found in Redis cache");
        return Collections.emptyList();
    }


    public void cacheAllEmployees(List<EmployeeDto> employees) {
        log.debug("Attempting to cache all employees list");
        try {
            if (employees == null || employees.isEmpty()) {
                log.warn("Cannot cache empty or null employees list");
                return;
            }

            employeeListRedisTemplate.opsForValue().set(ALL_EMPLOYEES_CACHE_KEY, employees, 1, TimeUnit.HOURS);
            log.info("CACHE UPDATE - Successfully cached {} employees in Redis", employees.size());
            log.debug("Cached employees: {}", employees);
        } catch (Exception e) {
            log.error("Error caching all employees", e);
        }
    }


    public void evictEmployee(Long id) {
        String key = EMPLOYEE_CACHE_KEY_PREFIX + id;
        log.debug("Attempting to evict employee from cache, key: {}", key);

        try {
            Boolean deleted = employeeRedisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("CACHE EVICT - Successfully removed employee ID: {} from Redis cache", id);
            } else {
                log.info("CACHE MISS - No cache entry found for employee ID: {}", id);
            }

            log.debug("Initiating eviction of all employees cache");
            evictAllEmployees();
            log.info("Cache eviction operations completed for employee ID: {}", id);
        } catch (Exception e) {
            log.error("Error evicting employee ID: {} from cache: {}", id, e.getMessage());
            log.debug("Error details:", e);
        }
    }

    public void evictAllEmployees() {
        log.debug("Attempting to evict all employees from cache");
        try {
            Boolean deleted = employeeListRedisTemplate.delete(ALL_EMPLOYEES_CACHE_KEY);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("CACHE EVICT - Successfully removed all employees from Redis cache");
            } else {
                log.info("CACHE MISS - No cache entry found for all employees");
            }
        } catch (Exception e) {
            log.error("Error evicting all employees cache: {}", e.getMessage());
            log.debug("Error details:", e);
        }
    }
    private void updateAllEmployeesCache() {
        evictAllEmployees();
    }
}
