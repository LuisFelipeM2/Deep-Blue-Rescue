package com.deepblue.rescue.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.RescueCaseMapper;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.service.RescueCaseService;

@Service 
@Transactional (readOnly = true)
public class RescueCaseServiceImpl
        implements RescueCaseService {

    private final RescueCaseRepository repository;

    private final RescueCaseMapper mapper;

    public RescueCaseServiceImpl(
            RescueCaseRepository repository,
            RescueCaseMapper mapper) {

        this.repository = repository;
        this.mapper = mapper;
    }
    @Override
    public RescueCaseResponse findByCode(
        String caseCode) {

        return repository
            .findByCaseCode(caseCode)
            .map(mapper::toResponse)
            .orElseThrow(
                () -> new ResourceNotFoundException(
                    "Rescue case not found: "
                    + caseCode
                )
            );
    }
        @Override
        public List<RescueCaseResponse> findByStatus(
            RescueStatus status) {

        return repository
                .findByStatusOrderByRescueDateAsc(status)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
        private boolean isValidTransition(
                RescueStatus current,
                RescueStatus next) {

            return switch (current) {

                case ADMITTED ->
                    next == RescueStatus.UNDER_EVALUATION;

                case UNDER_EVALUATION ->
                    next == RescueStatus.IN_REHABILITATION;

                case IN_REHABILITATION ->
                    next == RescueStatus.READY_FOR_RELEASE;

                case READY_FOR_RELEASE ->
                    next == RescueStatus.RELEASED;

                default -> false;
            };
        }
        @Override
        @Transactional
        public RescueCaseResponse changeStatus(
                String caseCode,
                ChangeRescueStatusRequest request) {

            
            RescueCase rescueCase = repository.findByCaseCode(caseCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Rescue case not found: " + caseCode));

            RescueStatus currentStatus = rescueCase.getStatus();
            RescueStatus nextStatus = request.status();

            if (!isValidTransition(currentStatus, nextStatus)) {
                throw new BusinessRuleException("Transición inválida de " + currentStatus + " a " + nextStatus);
            }

            rescueCase.setStatus(nextStatus);

            rescueCase = repository.save(rescueCase);
            return mapper.toResponse(rescueCase);
        }

    
}
