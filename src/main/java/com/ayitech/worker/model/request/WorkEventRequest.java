package com.ayitech.worker.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkEventRequest {

    // every step of the day - setting off, arriving, starting, pausing, resuming, finishing -
    // is the same shape: where the worker was when it happened, and anything they want to say
    // about it. one request rather than six near-identical ones, because what differs between
    // those steps is the route they arrive on and nothing on the body
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String notes;
}
