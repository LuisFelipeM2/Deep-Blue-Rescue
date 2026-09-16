package com.deepblue.rescue.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.AnimalMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.service.impl.AnimalServiceImpl;

@ExtendWith(MockitoExtension.class)
class AnimalServiceImplTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private AnimalMapper animalMapper;

    @InjectMocks
    private AnimalServiceImpl service;

    @Test
    void shouldFindAnimalByCode() {

        RescueCase rescueCase = new RescueCase(
                "RES-001", 
                java.time.LocalDate.now(), 
                "Santa Marta", 
                RescueStatus.IN_REHABILITATION
        );

        Animal animal = mock(Animal.class);
        //when(animal.getRescueCase()).thenReturn(rescueCase);

        AnimalResponse response = new AnimalResponse(
                1L, 
                "AN-001", 
                "Tortuga Marina", 
                "Chelonia mydas", 
                AnimalSex.MALE, 
                "RES-001", 
                RescueStatus.IN_REHABILITATION
        );

        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));

        when(animalMapper.toResponse(any()))
                .thenReturn(response);

        AnimalResponse result = service.findByCode("AN-001");

        assertThat(result).isEqualTo(response);

        verify(animalRepository).findByAnimalCode("AN-001");
        verify(animalMapper).toResponse(animal);
    }

    @Test
    void shouldThrowExceptionWhenAnimalNotFound() {

        when(animalRepository.findByAnimalCode("AN-999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("AN-999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Animal not found: AN-999");
    }

    @Test
    void shouldReturnTrueWhenAnimalCanReceiveTreatment() {

        RescueCase rescueCase = new RescueCase(
                "RES-001", 
                java.time.LocalDate.now(), 
                "Santa Marta", 
                RescueStatus.IN_REHABILITATION
        );

        Animal animal = mock(Animal.class);
        when(animal.getRescueCase()).thenReturn(rescueCase);

        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));

        boolean canTreat = service.canReceiveTreatment("AN-001");

        assertThat(canTreat).isTrue();

        verify(animalRepository).findByAnimalCode("AN-001");
    }
}