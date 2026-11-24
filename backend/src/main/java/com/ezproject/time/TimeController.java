package com.ezproject.time;

import java.time.LocalDate;

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

import com.ezproject.common.PageDto;
import com.ezproject.time.TimeDtos.EntryDto;
import com.ezproject.time.TimeDtos.EntryRequest;
import com.ezproject.time.TimeDtos.SummaryDto;
import com.ezproject.time.TimeDtos.WeekDto;
import com.ezproject.time.TimeDtos.WeekRequest;

import jakarta.validation.Valid;

@RestController
public class TimeController {

    private final TimeService service;

    public TimeController(TimeService service) {
        this.service = service;
    }

    @GetMapping("/api/projects/{projectId}/time/summary")
    public SummaryDto summary(@PathVariable Long projectId) {
        return service.summary(projectId);
    }

    @GetMapping("/api/projects/{projectId}/time")
    public PageDto<EntryDto> entries(@PathVariable Long projectId, @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "25") int size) {
        return service.entries(projectId, page, size);
    }

    @PostMapping("/api/projects/{projectId}/time")
    @ResponseStatus(HttpStatus.CREATED)
    public EntryDto log(@PathVariable Long projectId, @Valid @RequestBody EntryRequest request) {
        return service.log(projectId, request);
    }

    @PutMapping("/api/time/{entryId}")
    public EntryDto update(@PathVariable Long entryId, @Valid @RequestBody EntryRequest request) {
        return service.update(entryId, request);
    }

    @DeleteMapping("/api/time/{entryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long entryId) {
        service.delete(entryId);
    }

    @GetMapping("/api/time/week")
    public WeekDto myWeek(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.myWeek(date == null ? LocalDate.now() : date);
    }

    @PutMapping("/api/time/week")
    public WeekDto saveWeek(@Valid @RequestBody WeekRequest request) {
        return service.saveWeek(request);
    }
}
