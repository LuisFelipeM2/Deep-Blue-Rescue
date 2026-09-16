package com.deepblue.rescue.dto.response;

import java.time.LocalDate;

import com.deepblue.rescue.domain.RescueStatus;

public record RescueCaseResponse(Long id,

        String caseCode,

        LocalDate rescueDate,

        String rescueLocation,

        RescueStatus status,

        String centerCode,

        String animalCode) {
} 
  

