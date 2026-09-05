package com.deepblue.rescue.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "medical_records")
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "initial_weight", nullable = false)
    private BigDecimal initialWeight;

    @Column(name = "initial_condition", nullable = false)
    private String initialCondition;

    @Column(columnDefinition = "TEXT")
    private String injuries;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false, unique = true)
    private Animal animal;

    protected MedicalRecord() {
    }

    public MedicalRecord(BigDecimal initialWeight, String initialCondition, String injuries, String observations) {
        this.initialWeight = initialWeight;
        this.initialCondition = initialCondition;
        this.injuries = injuries;
        this.observations = observations;
    }

    void setAnimal(Animal animal) {
        this.animal = animal;
    }

    // Getters

    public Long getId() {
        return id;
    }

    public BigDecimal getInitialWeight() {
        return initialWeight;
    }

    public String getInitialCondition() {
        return initialCondition;
    }

    public String getInjuries() {
        return injuries;
    }

    public String getObservations() {
        return observations;
    }

    public Animal getAnimal() {
        return animal;
    }

    // Setters (campos que pueden actualizarse durante el tratamiento)

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public void setInjuries(String injuries) {
        this.injuries = injuries;
    }
}