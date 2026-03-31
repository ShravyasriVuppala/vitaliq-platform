package com.vitaliq.vitaliq_platform.dto.dashboard;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class VolumeDataPoint {

    private LocalDate date;          // the day — e.g. 2026-03-27
    private Double totalVolumeLbs;   // total volume lifted that day
}