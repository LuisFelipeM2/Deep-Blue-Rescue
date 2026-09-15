package com.deepblue.rescue.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "animals")
public class Animal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "animal_code", nullable = false, unique = true)
    private String animalCode;

    @Column(name = "common_name", nullable = false)
    private String commonName;

    @Column(name = "scientific_name", nullable = false)
    private String scientificName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnimalSex sex;

    @Column(name = "tracking_device_code", unique = true)
    private String trackingDeviceCode;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rescue_case_id", unique = true)
    private RescueCase rescueCase;

    @OneToOne(
        mappedBy = "animal",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private MedicalRecord medicalRecord;

    @OneToMany(mappedBy = "animal")
    private List<Treatment> treatments = new ArrayList<>();

    protected Animal() {
        // requerido por JPA
    }

    public Animal(String animalCode, String commonName, String scientificName, AnimalSex sex) {
        this.animalCode = animalCode;
        this.commonName = commonName;
        this.scientificName = scientificName;
        this.sex = sex;
    }

    void setRescueCase(RescueCase rescueCase) {
        this.rescueCase = rescueCase;
    }

    public void assignMedicalRecord(MedicalRecord medicalRecord) {
        this.medicalRecord = medicalRecord;
        medicalRecord.setAnimal(this);
    }

    // Getters

    public Long getId() {
        return id;
    }

    public String getAnimalCode() {
        return animalCode;
    }

    public String getCommonName() {
        return commonName;
    }

    public String getScientificName() {
        return scientificName;
    }

    public AnimalSex getSex() {
        return sex;
    }

    public String getTrackingDeviceCode() {
        return trackingDeviceCode;
    }

    public RescueCase getRescueCase() {
        return rescueCase;
    }

    public MedicalRecord getMedicalRecord() {
        return medicalRecord;
    }

    public List<Treatment> getTreatments() {
        return treatments;
    }

    // Setters

    public void setTrackingDeviceCode(String trackingDeviceCode) {
        this.trackingDeviceCode = trackingDeviceCode;
    }
}