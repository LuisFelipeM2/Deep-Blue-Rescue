package com.deepblue.rescue;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.Expertise;
import com.deepblue.rescue.domain.MedicalRecord;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueCenter;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.ExpertiseRepository;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.repository.RescueCenterRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest
@Transactional
class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:18-alpine"))
                    .withDatabaseName("deepblue_test")
                    .withUsername("deepblue")
                    .withPassword("deepblue");

    @Autowired
    private RescueCenterRepository rescueCenterRepository;

    @Autowired
    private RescueCaseRepository rescueCaseRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private SpecialistRepository specialistRepository;

    @Autowired
    private ExpertiseRepository expertiseRepository;

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayShouldHaveExecutedMigrations() {
    List<String> versions = jdbcTemplate.queryForList(
            "SELECT version FROM flyway_schema_history ORDER BY installed_rank",
            String.class);

    assertThat(versions).contains("1", "2");
    }

    @Test
    void shouldPersistAndRetrieveRescueCenterUsingInheritedMethods() {
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");

        RescueCenter saved = rescueCenterRepository.save(center);

        assertThat(saved.getId()).isNotNull();
        assertThat(rescueCenterRepository.existsById(saved.getId())).isTrue();
        assertThat(rescueCenterRepository.findById(saved.getId())).isPresent();
        assertThat(rescueCenterRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldAssociateMultipleRescueCasesWithSameCenter() {
        RescueCenter center = new RescueCenter("DB-CAR2", "DeepBlue Caribbean Center 2", "Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase case1 = new RescueCase("RES-2026-100", LocalDate.now(), "Playa Blanca", RescueStatus.ADMITTED);
        RescueCase case2 = new RescueCase("RES-2026-101", LocalDate.now(), "Bahía Concha", RescueStatus.ADMITTED);

        center.addCase(case1);
        center.addCase(case2);

        rescueCaseRepository.save(case1);
        rescueCaseRepository.save(case2);

        List<RescueCase> cases = rescueCaseRepository.findByRescueCenterCode("DB-CAR2");

        assertThat(cases).hasSize(2);
        assertThat(cases).allMatch(c -> c.getRescueCenter().getCode().equals("DB-CAR2"));
        }

        @Test
        void shouldAssignAnimalToRescueCase() {
            RescueCenter center = new RescueCenter("DB-CAR3", "DeepBlue Caribbean Center 3", "Santa Marta");
            rescueCenterRepository.save(center);

            RescueCase rescueCase = new RescueCase("RES-2026-001", LocalDate.now(), "Taganga", RescueStatus.ADMITTED);
            center.addCase(rescueCase);
            rescueCaseRepository.save(rescueCase);

            Animal animal = new Animal("AN-2026-001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.UNKNOWN);
            rescueCase.assignAnimal(animal);
            animalRepository.save(animal);

            RescueCase persistedCase = rescueCaseRepository.findByCaseCode("RES-2026-001").orElseThrow();
            Animal persistedAnimal = animalRepository.findByAnimalCode("AN-2026-001").orElseThrow();

            assertThat(persistedCase.getAnimal().getAnimalCode()).isEqualTo("AN-2026-001");
            assertThat(persistedAnimal.getRescueCase().getCaseCode()).isEqualTo("RES-2026-001");
        }
        @Test
        void shouldPersistMedicalRecordViaCascade() {
        RescueCenter center = new RescueCenter("DB-MED1", "Centro Medical", "Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase("RES-MED-001", LocalDate.now(), "Taganga", RescueStatus.ADMITTED);
        center.addCase(rescueCase);
        rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal("AN-2026-002", "Green Sea Turtle", "Chelonia mydas", AnimalSex.UNKNOWN);
        rescueCase.assignAnimal(animal);

        MedicalRecord medicalRecord = new MedicalRecord(
            new BigDecimal("28.40"),
            "STABLE",
            "Left front flipper injury",
            null);

        animal.assignMedicalRecord(medicalRecord);}
        @Test
        void shouldAssociateSpecialistWithMultipleExpertiseAreas() {    
            Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
            Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();

            Specialist elena = new Specialist("SP-001", "Elena", "Vargas", "elena.vargas@deepblue.com", true);
            elena.addExpertise(trauma);
            elena.addExpertise(rehabilitation);

            Specialist savedElena = specialistRepository.save(elena);

            Specialist persistedElena = specialistRepository.findById(savedElena.getId()).orElseThrow();

            assertThat(persistedElena.getExpertiseAreas()).hasSize(2);
        }

        @Test
        void shouldFindRescueCasesByStatus() {
            RescueCenter center = new RescueCenter("DB-CAR4", "DeepBlue Caribbean Center 4", "Santa Marta");
            rescueCenterRepository.save(center);

            RescueCase case1 = new RescueCase("RES-001", LocalDate.now(), "Taganga", RescueStatus.IN_REHABILITATION);
            RescueCase case2 = new RescueCase("RES-002", LocalDate.now(), "Taganga", RescueStatus.READY_FOR_RELEASE);
            RescueCase case3 = new RescueCase("RES-003", LocalDate.now(), "Taganga", RescueStatus.IN_REHABILITATION);

            center.addCase(case1);
            center.addCase(case2);
            center.addCase(case3);

            rescueCaseRepository.save(case1);
            rescueCaseRepository.save(case2);
            rescueCaseRepository.save(case3);

            List<RescueCase> inRehabilitation =
             rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION);

             assertThat(inRehabilitation).hasSize(2);
        }

        @Test
            void shouldFindAnimalsOnlyFromSpecifiedCenter() {
            RescueCenter carCenter = new RescueCenter("DB-CAR5", "DeepBlue Caribbean Center 5", "Santa Marta");
            RescueCenter pacCenter = new RescueCenter("DB-PAC5", "DeepBlue Pacific Center 5", "Buenaventura");
            rescueCenterRepository.save(carCenter);
            rescueCenterRepository.save(pacCenter);

            RescueCase carCase = new RescueCase("RES-CAR-001", LocalDate.now(), "Taganga", RescueStatus.ADMITTED);
            RescueCase pacCase = new RescueCase("RES-PAC-001", LocalDate.now(), "Buenaventura", RescueStatus.ADMITTED);

            carCenter.addCase(carCase);
            pacCenter.addCase(pacCase);

            rescueCaseRepository.save(carCase);
            rescueCaseRepository.save(pacCase);

            Animal carAnimal = new Animal("AN-CAR-001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.UNKNOWN);
            Animal pacAnimal = new Animal("AN-PAC-001", "Olive Ridley Turtle", "Lepidochelys olivacea", AnimalSex.UNKNOWN);

            carCase.assignAnimal(carAnimal);
            pacCase.assignAnimal(pacAnimal);

            animalRepository.save(carAnimal);
            animalRepository.save(pacAnimal);

            List<Animal> carAnimals = animalRepository.findByRescueCaseRescueCenterCode("DB-CAR5");

            assertThat(carAnimals).hasSize(1);
            assertThat(carAnimals).extracting(Animal::getAnimalCode).containsExactly("AN-CAR-001");
        }
        @Test
        void shouldFindActiveSpecialistsByExpertise() {
            Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
            Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();
            Expertise marineMammals = expertiseRepository.findByNameIgnoreCase("Marine Mammals").orElseThrow();
            Expertise marineBirds = expertiseRepository.findByNameIgnoreCase("Marine Birds").orElseThrow();

            Specialist elena = new Specialist("SP-101", "Elena", "Vargas", "elena.101@deepblue.com", true);
            elena.addExpertise(trauma);
            elena.addExpertise(rehabilitation);

            Specialist mateo = new Specialist("SP-102", "Mateo", "Rojas", "mateo.102@deepblue.com", true);
            mateo.addExpertise(marineMammals);
            mateo.addExpertise(rehabilitation);

            Specialist sofia = new Specialist("SP-103", "Sofia", "Gomez", "sofia.103@deepblue.com", true);
            sofia.addExpertise(marineBirds);
            sofia.addExpertise(trauma);

            specialistRepository.save(elena);
            specialistRepository.save(mateo);
            specialistRepository.save(sofia);

            List<Specialist> traumaSpecialists = specialistRepository.findActiveByExpertise("Trauma");

            assertThat(traumaSpecialists).hasSize(2);
            assertThat(traumaSpecialists).extracting(Specialist::getFirstName)
            .containsExactlyInAnyOrder("Elena", "Sofia");
        }

        @Test
        void shouldPersistTreatmentsForAnimal() {
            RescueCenter center = new RescueCenter("DB-CAR6", "DeepBlue Caribbean Center 6", "Santa Marta");
            rescueCenterRepository.save(center);

            RescueCase rescueCase = new RescueCase("RES-TRT-001", LocalDate.now(), "Taganga", RescueStatus.IN_REHABILITATION);
            center.addCase(rescueCase);
            rescueCaseRepository.save(rescueCase);

            Animal animal = new Animal("AN-TRT-001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.UNKNOWN);
            rescueCase.assignAnimal(animal);
            animalRepository.save(animal);

            Specialist elena = new Specialist("SP-201", "Elena", "Vargas", "elena.201@deepblue.com", true);
            Specialist mateo = new Specialist("SP-202", "Mateo", "Rojas", "mateo.202@deepblue.com", true);
            specialistRepository.save(elena);
            specialistRepository.save(mateo);

            Treatment treatment1 = new Treatment(animal, elena,
                    LocalDateTime.of(2026, 8, 1, 9, 0), TreatmentType.WOUND_CARE, "Treatment 1");

            Treatment treatment2 = new Treatment(animal, elena,
                    LocalDateTime.of(2026, 8, 2, 9, 0), TreatmentType.HYDRATION, "Treatment 2");

            Treatment treatment3 = new Treatment(animal, mateo,
                    LocalDateTime.of(2026, 8, 3, 9, 0), TreatmentType.OBSERVATION, "Treatment 3");

            treatmentRepository.save(treatment1);
            treatmentRepository.save(treatment2);
            treatmentRepository.save(treatment3);

            assertThat(treatmentRepository.count()).isEqualTo(3);
        }
        @Test
        void shouldFindTreatmentsBetweenDates() {
            RescueCenter center = new RescueCenter("DB-CAR7", "DeepBlue Caribbean Center 7", "Santa Marta");
            rescueCenterRepository.save(center);

            RescueCase rescueCase = new RescueCase("RES-TRT-002", LocalDate.now(), "Taganga", RescueStatus.IN_REHABILITATION);
            center.addCase(rescueCase);
            rescueCaseRepository.save(rescueCase);

            Animal animal = new Animal("AN-TRT-002", "Green Sea Turtle", "Chelonia mydas", AnimalSex.UNKNOWN);
            rescueCase.assignAnimal(animal);
            animalRepository.save(animal);

            Specialist elena = new Specialist("SP-301", "Elena", "Vargas", "elena.301@deepblue.com", true);
            specialistRepository.save(elena);

            Treatment early = new Treatment(animal, elena,
                    LocalDateTime.of(2026, 8, 1, 10, 0), TreatmentType.WOUND_CARE, "Early");

            Treatment middle = new Treatment(animal, elena,
                    LocalDateTime.of(2026, 8, 10, 10, 0), TreatmentType.HYDRATION, "Middle");

            Treatment late = new Treatment(animal, elena,
                    LocalDateTime.of(2026, 8, 20, 10, 0), TreatmentType.OBSERVATION, "Late");

            treatmentRepository.save(early);
            treatmentRepository.save(middle);
            treatmentRepository.save(late);

            List<Treatment> result = treatmentRepository.findByPerformedAtBetween(
                    LocalDateTime.of(2026, 8, 5, 0, 0),
                    LocalDateTime.of(2026, 8, 15, 0, 0));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDescription()).isEqualTo("Middle");
        }
        @Test
        void shouldViolateUniqueConstraintOnAnimalCode() {
        RescueCenter center = new RescueCenter("DB-UQ1", "Centro Unique", "Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase case1 = new RescueCase("RES-UQ-001", LocalDate.now(), "Taganga", RescueStatus.ADMITTED);
        center.addCase(case1);
        rescueCaseRepository.save(case1);

        Animal first = new Animal("AN-100", "Green Sea Turtle", "Chelonia mydas", AnimalSex.UNKNOWN);
        case1.assignAnimal(first);
        animalRepository.saveAndFlush(first);

        RescueCase case2 = new RescueCase("RES-UQ-002", LocalDate.now(), "Taganga", RescueStatus.ADMITTED);
        center.addCase(case2);
        rescueCaseRepository.save(case2);

        Animal duplicate = new Animal("AN-100", "Loggerhead Turtle", "Caretta caretta", AnimalSex.UNKNOWN);
        case2.assignAnimal(duplicate);

        assertThrows(DataIntegrityViolationException.class, () -> {
            animalRepository.saveAndFlush(duplicate);
        });
    }

        @Test
        void shouldPersistFullIntegrationScenario() {
            // Centro
            RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta");
            rescueCenterRepository.save(center);

            // Caso
            RescueCase rescueCase = new RescueCase(
                    "RES-2026-100",
                    LocalDate.of(2026, 8, 18),
                    "Bahía Concha",
                    RescueStatus.IN_REHABILITATION);
            center.addCase(rescueCase);
            rescueCaseRepository.save(rescueCase);

            // Animal
            Animal animal = new Animal("AN-2026-100", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
            rescueCase.assignAnimal(animal);
            animalRepository.save(animal);

            // Expediente médico
            MedicalRecord medicalRecord = new MedicalRecord(
                    new BigDecimal("27.80"),
                    "STABLE",
                    "Injury caused by fishing net",
                    "Possible plastic ingestion");
            animal.assignMedicalRecord(medicalRecord);
            animalRepository.save(animal);

            // Especialista
            Expertise marineReptiles = expertiseRepository.findByNameIgnoreCase("Marine Reptiles").orElseThrow();
            Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
            Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();

            Specialist elena = new Specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org", true);
            elena.addExpertise(marineReptiles);
            elena.addExpertise(trauma);
            elena.addExpertise(rehabilitation);
            specialistRepository.save(elena);

            // Tratamientos
            Treatment treatment1 = new Treatment(
                    animal, elena,
                    LocalDateTime.of(2026, 8, 19, 9, 0),
                    TreatmentType.WOUND_CARE,
                    "Cleaning of left front flipper");

            Treatment treatment2 = new Treatment(
                    animal, elena,
                    LocalDateTime.of(2026, 8, 20, 9, 0),
                    TreatmentType.HYDRATION,
                    "Subcutaneous fluid therapy");

            treatmentRepository.save(treatment1);
            treatmentRepository.save(treatment2);

            // Verificaciones básicas
            assertThat(animal.getId()).isNotNull();
            assertThat(animal.getMedicalRecord().getId()).isNotNull();
            assertThat(elena.getExpertiseAreas()).hasSize(3);
            assertThat(treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId())).hasSize(2);
        }
                private void createFullIntegrationScenario() {
            RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta");
            rescueCenterRepository.save(center);

            RescueCase rescueCase = new RescueCase(
                    "RES-2026-100", LocalDate.of(2026, 8, 18), "Bahía Concha", RescueStatus.IN_REHABILITATION);
            center.addCase(rescueCase);
            rescueCaseRepository.save(rescueCase);

            Animal animal = new Animal("AN-2026-100", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
            rescueCase.assignAnimal(animal);
            animalRepository.save(animal);

            MedicalRecord medicalRecord = new MedicalRecord(
                    new BigDecimal("27.80"), "STABLE",
                    "Injury caused by fishing net", "Possible plastic ingestion");
            animal.assignMedicalRecord(medicalRecord);
            animalRepository.save(animal);

            Expertise marineReptiles = expertiseRepository.findByNameIgnoreCase("Marine Reptiles").orElseThrow();
            Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
            Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();

            Specialist elena = new Specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org", true);
            elena.addExpertise(marineReptiles);
            elena.addExpertise(trauma);
            elena.addExpertise(rehabilitation);
            specialistRepository.save(elena);

            Treatment treatment1 = new Treatment(animal, elena,
                    LocalDateTime.of(2026, 8, 19, 9, 0), TreatmentType.WOUND_CARE, "Cleaning of left front flipper");
            Treatment treatment2 = new Treatment(animal, elena,
                    LocalDateTime.of(2026, 8, 20, 9, 0), TreatmentType.HYDRATION, "Subcutaneous fluid therapy");

            treatmentRepository.save(treatment1);
            treatmentRepository.save(treatment2);
        }
        @Test
        void query1_shouldCheckIfCaseExists() {
            createFullIntegrationScenario();
            boolean exists = rescueCaseRepository.findByCaseCode("RES-2026-100").isPresent();
            assertThat(exists).isTrue();
        }

        @Test
        void query2_shouldFindCasesInRehabilitation() {
            createFullIntegrationScenario();
            List<RescueCase> cases = rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION);
            assertThat(cases).extracting(RescueCase::getCaseCode).contains("RES-2026-100");
        }

        @Test
        void query3_shouldFindAnimalsFromCenter() {
            createFullIntegrationScenario();
            List<Animal> animals = animalRepository.findByRescueCaseRescueCenterCode("DB-CAR");
            assertThat(animals).extracting(Animal::getAnimalCode).contains("AN-2026-100");
        }

        @Test
        void query4_shouldFindAnimalsByCommonNameContaining() {
            createFullIntegrationScenario();
            List<Animal> animals = animalRepository.findByCommonNameContainingIgnoreCase("turtle");
            assertThat(animals).extracting(Animal::getAnimalCode).contains("AN-2026-100");
        }

        @Test
        void query5_shouldFindSpecialistsByExpertise() {
            createFullIntegrationScenario();
            List<Specialist> specialists = specialistRepository.findActiveByExpertise("Trauma");
            assertThat(specialists).extracting(Specialist::getFirstName).contains("Elena");
        }

        @Test
        void query7_shouldFindTreatmentsBySpecialistExpertise() {
            createFullIntegrationScenario();
            List<Treatment> treatments = treatmentRepository.findBySpecialistExpertise("Rehabilitation");
            assertThat(treatments).hasSize(2);
        }

        @Test
        void query8_shouldFindTreatmentsBetweenDates() {
            createFullIntegrationScenario();
            List<Treatment> treatments = treatmentRepository.findByPerformedAtBetween(
                    LocalDateTime.of(2026, 8, 18, 0, 0),
                    LocalDateTime.of(2026, 8, 21, 0, 0));
            assertThat(treatments).hasSize(2);
        }
        @Test
void deberiaEncontrarAnimalesEnRehabilitacionTratadosPorEspecialistaConExpertise() {

    
    Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();

    Specialist drRios = new Specialist("SP-001", "Ana", "Rios", "ana.rios@deepblue.com", true);
    drRios.getExpertiseAreas().add(trauma);
    specialistRepository.save(drRios);

    RescueCenter center = new RescueCenter("DB-CAR", "Centro Caribe", "Cartagena");
    rescueCenterRepository.save(center);

    RescueCase rescueCase = new RescueCase(
        "RC-2026-001", LocalDate.now(), "Playa X", RescueStatus.IN_REHABILITATION
    );
    center.addCase(rescueCase);
    rescueCaseRepository.save(rescueCase);

    Animal turtle = new Animal("AN-2026-100", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
    rescueCase.assignAnimal(turtle);
    animalRepository.save(turtle);

    Treatment treatment = new Treatment(
        turtle, drRios, LocalDateTime.now(), TreatmentType.SURGERY, "Cirugía de aleta"
    );
    treatmentRepository.save(treatment);

    // --- Arrange: escenario que NO debe cumplir (control negativo) ---
    Specialist drGomez = new Specialist("SP-002","Luis","Gomez","luis.gomez@deepblue.com",true);
    specialistRepository.save(drGomez);

    RescueCase otherCase = new RescueCase(
        "RC-2026-002", LocalDate.now(), "Playa Y", RescueStatus.RELEASED
    );
    center.addCase(otherCase);
    rescueCaseRepository.save(otherCase);

    Animal dolphin = new Animal("AN-2026-200", "Dolphin", "Tursiops truncatus", AnimalSex.MALE);
    otherCase.assignAnimal(dolphin);
    animalRepository.save(dolphin);

    Treatment otherTreatment = new Treatment(
        dolphin, drGomez, LocalDateTime.now(), TreatmentType.OBSERVATION, "Revisión general"
    );
    treatmentRepository.save(otherTreatment);

    // --- Act ---
    List<Animal> resultado = animalRepository
        .findInRehabilitationTreatedBySpecialistWithExpertise(
            RescueStatus.IN_REHABILITATION,
            "trauma"
        );

    // --- Assert ---
    assertThat(resultado)
        .extracting(Animal::getAnimalCode)
        .containsExactly("AN-2026-100");
}
       
}
