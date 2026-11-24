package com.ezproject.capacity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ezproject.calendar.HolidayCalendar;
import com.ezproject.capacity.CapacityDtos.AbsenceDto;
import com.ezproject.capacity.CapacityDtos.AbsenceRequest;
import com.ezproject.capacity.CapacityDtos.AllocationDto;
import com.ezproject.capacity.CapacityDtos.AllocationRequest;
import com.ezproject.capacity.CapacityDtos.CapacityDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@RestController
public class CapacityController {

    private final CapacityService service;

    public CapacityController(CapacityService service) {
        this.service = service;
    }

    public record CapacityRequest(@NotNull @DecimalMin("0") @DecimalMax("5") BigDecimal daysPerWeek) {
    }

    public record CalendarRequest(@NotNull HolidayCalendar calendar) {
    }

    @GetMapping("/api/capacity")
    public CapacityDto capacity(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                @RequestParam(defaultValue = "8") int weeks,
                                @RequestParam(required = false) Long teamId) {
        return service.capacity(from == null ? LocalDate.now() : from, weeks, teamId);
    }

    @GetMapping("/api/absences")
    public List<AbsenceDto> absences(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.absencesInRange(from, to);
    }

    @GetMapping("/api/absences/mine")
    public List<AbsenceDto> myAbsences() {
        return service.myAbsences();
    }

    @PostMapping("/api/absences")
    @ResponseStatus(HttpStatus.CREATED)
    public AbsenceDto createAbsence(@Valid @RequestBody AbsenceRequest request) {
        return service.createAbsence(request);
    }

    @DeleteMapping("/api/absences/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAbsence(@PathVariable Long id) {
        service.deleteAbsence(id);
    }

    @GetMapping("/api/projects/{projectId}/allocations")
    public List<AllocationDto> allocations(@PathVariable Long projectId) {
        return service.projectAllocations(projectId);
    }

    @PostMapping("/api/projects/{projectId}/allocations")
    @ResponseStatus(HttpStatus.CREATED)
    public AllocationDto allocate(@PathVariable Long projectId, @Valid @RequestBody AllocationRequest request) {
        return service.allocate(projectId, request);
    }

    @PutMapping("/api/allocations/{id}")
    public AllocationDto updateAllocation(@PathVariable Long id, @Valid @RequestBody AllocationRequest request) {
        return service.updateAllocation(id, request);
    }

    @DeleteMapping("/api/allocations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllocation(@PathVariable Long id) {
        service.deleteAllocation(id);
    }

    @PutMapping("/api/users/{userId}/capacity")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeCapacity(@PathVariable Long userId, @Valid @RequestBody CapacityRequest request) {
        service.changeCapacity(userId, request.daysPerWeek());
    }

    @PutMapping("/api/organization/holiday-calendar")
    public CalendarRequest changeCalendar(@Valid @RequestBody CalendarRequest request) {
        return new CalendarRequest(service.changeHolidayCalendar(request.calendar()));
    }
}
