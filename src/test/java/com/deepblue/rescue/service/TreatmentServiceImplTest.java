package com.deepblue.rescue.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.impl.TreatmentServiceImpl;

@ExtendWith (MockitoExtension.class)
public class TreatmentServiceImplTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private SpecialistRepository specialistRepository;

    @Mock
    private TreatmentRepository treatmentRepository;

    @Mock
    private TreatmentMapper mapper; // El mapper que convierte la entidad a response

    @InjectMocks
    private TreatmentServiceImpl service;

@Test
void shouldRegisterTreatmentSuccessfully() {

    RescueCase rescueCase = new RescueCase(
            "RES-001", 
            java.time.LocalDate.now(), 
            "Bahía", 
            RescueStatus.IN_REHABILITATION
    );


    Animal animal = new Animal("AN-001", 
            "Tortuga Marina", 
            "Chelonia mydas", 
            AnimalSex.MALE);
        

  
    Specialist specialist = new Specialist(
            "SPEC-001",             
            "Carlos",               
            "López",                
            "clopez@gmail.com",       
            true                    
    );

    CreateTreatmentRequest request = new CreateTreatmentRequest(
            "AN-001",
            "SPEC-001",
            java.time.LocalDateTime.now(), 
            TreatmentType.WOUND_CARE,
            "Limpieza de aleta derecha"
    );


    TreatmentResponse response = new TreatmentResponse(
            1L, 
            "AN-001", 
            "SPEC-001", 
            java.time.LocalDateTime.now(), 
            TreatmentType.WOUND_CARE, 
            "Limpieza de aleta derecha"
    );

    
    when(animalRepository.findByAnimalCode("AN-001"))
            .thenReturn(Optional.of(animal));
            
    when(specialistRepository.findByProfessionalCode("SPEC-001"))
            .thenReturn(Optional.of(specialist));

    when(treatmentRepository.save(any(Treatment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
            
    when(mapper.toResponse(any(Treatment.class)))
            .thenReturn(response);


   
     Animal animal = mock(Animal.class);
        when(animal.getRescueCase()).thenReturn(rescueCase);
    TreatmentResponse result = service.register(request);



    assertThat(result).isEqualTo(response);

    
    verify(treatmentRepository).save(any(Treatment.class));
}

@Test
    void shouldThrowExceptionWhenSpecialistIsInactive() {

      

        Animal animal = mock(Animal.class);
        

        Specialist specialist = new Specialist(
                "SPEC-001",            
                "Carlos",              
                "López",               
                "clopez@gmail.com",      
                false 
        );

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001",
                "SPEC-001",
                java.time.LocalDateTime.now(), 
                TreatmentType.WOUND_CARE,
                "Limpieza"
        );

        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));
                

        when(specialistRepository.findByProfessionalCode("SPEC-001"))
        .thenReturn(Optional.of(specialist));

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(treatmentRepository, never()).save(any());
    }
@Test
    void shouldThrowExceptionWhenAnimalRescueCaseIsReleased() {

        RescueCase rescueCase = new RescueCase(
                "RES-001", 
                java.time.LocalDate.now().minusDays(10), 
                "Bahía", 
                RescueStatus.RELEASED 
        );

        Animal animal = mock(Animal.class);
        when(animal.getRescueCase()).thenReturn(rescueCase);

        Specialist specialist = new Specialist(
                "SPEC-001",            
                "Carlos",              
                "López",               
                "clopez@gmail.com",      
                true                   
        );

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001",
                "SPEC-001",
                java.time.LocalDateTime.now(), 
                TreatmentType.OBSERVATION, 
                "Revisión final"
        );

        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));
                
        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist));

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(treatmentRepository, never()).save(any());
    }
    @Test
    void shouldThrowExceptionWhenTreatmentDateIsBeforeRescueDate() {

        
        RescueCase rescueCase = new RescueCase(
                "RES-2026-100", 
                java.time.LocalDate.of(2026, 8, 20), 
                "Santa Marta", 
                RescueStatus.IN_REHABILITATION
        );

        Animal animal = mock(Animal.class);
        when(animal.getRescueCase()).thenReturn(rescueCase);

        Specialist specialist = new Specialist(
                "SPEC-001",            
                "Elena",              
                "Vargas",               
                "evargas@gmail.com",      
                true                   
        );

        
        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-2026-100",
                "SPEC-001",
                java.time.LocalDateTime.of(2026, 8, 15, 9, 0), 
                TreatmentType.WOUND_CARE,
                "Cleaning of left front flipper injury."
        );

        when(animalRepository.findByAnimalCode("AN-2026-100"))
                .thenReturn(Optional.of(animal));
                
        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist));

        
        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenAnimalIsReleasedForObservation() {

        
        

        Animal animal = mock(Animal.class);
       

        Specialist specialist = new Specialist(
                "SPEC-001",            
                "Elena",              
                "Vargas",               
                "evargas@gmail.com",      
                true                   
        );

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-2026-100",
                "SPEC-001",
                java.time.LocalDateTime.of(2026, 8, 21, 9, 0), 
                TreatmentType.OBSERVATION, 
                "Post-release check"
        );

        when(animalRepository.findByAnimalCode("AN-2026-100"))
                .thenReturn(Optional.of(animal));
                
        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist));

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenSpecialistIsDeactivatedForNewTreatment() {

        RescueCase rescueCase = new RescueCase(
                "RES-2026-100", 
                java.time.LocalDate.of(2026, 8, 20), 
                "Santa Marta", 
                RescueStatus.IN_REHABILITATION
        );

        Animal animal = mock(Animal.class);
        when(animal.getRescueCase()).thenReturn(rescueCase);

        
        Specialist specialist = new Specialist(
                "SPEC-001",            
                "Elena",              
                "Vargas",               
                "evargas@gmail.com",      
                false 
        );

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-2026-100",
                "SPEC-001",
                java.time.LocalDateTime.of(2026, 8, 21, 9, 0), 
                TreatmentType.WOUND_CARE,
                "Cleaning of left front flipper injury."
        );
        when(specialistRepository.findByProfessionalCode("SPEC-001"))
        .thenReturn(Optional.of(specialist));

        when(animalRepository.findByAnimalCode("AN-2026-100"))
                .thenReturn(Optional.of(animal));

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(treatmentRepository, never()).save(any());
    }
}