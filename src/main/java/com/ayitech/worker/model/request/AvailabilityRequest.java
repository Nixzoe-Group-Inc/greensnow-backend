package com.ayitech.worker.model.request;

import com.ayitech.worker.model.enums.AvailabilityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityRequest {

    private AvailabilityStatus availabilityStatus;
}
