package com.deepblue.rescue.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.TreatmentService;

@Service 
@Transactional (readOnly = true)
public class TreatmentServiceImpl
        implements TreatmentService {

    private final AnimalRepository animalRepository;

    private final SpecialistRepository specialistRepository;

    private final TreatmentRepository treatmentRepository;

    private final TreatmentMapper mapper;

    public TreatmentServiceImpl(
            AnimalRepository animalRepository,
            SpecialistRepository specialistRepository,
            TreatmentRepository treatmentRepository,
            TreatmentMapper mapper) {

        this.animalRepository = animalRepository;
        this.specialistRepository = specialistRepository;
        this.treatmentRepository = treatmentRepository;
        this.mapper = mapper;
    }
    @Override
    public List<TreatmentResponse> findByAnimalCode(String animalCode) {
    return treatmentRepository
            .findByAnimalAnimalCodeOrderByPerformedAtAsc(animalCode)
            .stream()
            .map(mapper::toResponse)
            .toList();
    }
    @Override
@Transactional
public TreatmentResponse register(CreateTreatmentRequest request) {

    Animal animal = animalRepository.findByAnimalCode(request.animalCode())
            .orElseThrow(() -> new ResourceNotFoundException("Animal not found: " + request.animalCode()));

    Specialist specialist = specialistRepository.findByProfessionalCode(request.specialistCode())
            .orElseThrow(() -> new ResourceNotFoundException("specialist not found: " + request.specialistCode()));

    
    if (!specialist.isActive()) {
        throw new BusinessRuleException("the specialist " + request.specialistCode() + " is not active.");
    }

    
    RescueCase rescueCase = animal.getRescueCase(); 

    if (rescueCase.getStatus() == RescueStatus.RELEASED || rescueCase.getStatus() == RescueStatus.CLOSED) {
        throw new BusinessRuleException("No se pueden agregar tratamientos porque el caso ya fue liberado.");
    }

    if (request.performedAt().toLocalDate().isBefore(rescueCase.getRescueDate())) {
        throw new BusinessRuleException("La fecha del tratamiento no puede ser anterior a la fecha de rescate.");
    }

 
    Treatment treatment = new Treatment(
            animal,
            specialist,
            request.performedAt(),
            request.type(),
            request.description()
    );

    treatment = treatmentRepository.save(treatment);

    return mapper.toResponse(treatment);
}
}
