package com.ayitech.worker.model.request;

import com.ayitech.worker.model.enums.AvailabilityStatus;
import com.ayitech.worker.model.enums.EducationLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeProfileRequest {

    private String headline;
    private String bio;
    private Integer yearsOfExperience;
    private EducationLevel educationLevel;
    private AvailabilityStatus availabilityStatus;
    private BigDecimal hourlyRate;
    private BigDecimal dailyRate;
    private String baseAddress;
    private BigDecimal baseLatitude;
    private BigDecimal baseLongitude;
    private Integer serviceRadiusKm;
    private Boolean hasOwnTools;
    private Boolean hasTransport;
}
