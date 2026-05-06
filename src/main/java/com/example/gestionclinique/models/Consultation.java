package com.example.gestionclinique.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Consultation {
    private int id;
    private int patientId;
    private String patientNom;
    private String patientPrenom;
    private int priorite;
    private String statut;
    private LocalDateTime dateConsultation;
    private Integer tensionSystolique;
    private Integer tensionDiastolique;
    private Double glycemie;
    private Double temperature;

    public Consultation() {}

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public String getPatientNom() { return patientNom; }
    public void setPatientNom(String patientNom) { this.patientNom = patientNom; }

    public String getPatientPrenom() { return patientPrenom; }
    public void setPatientPrenom(String patientPrenom) { this.patientPrenom = patientPrenom; }

    public int getPriorite() { return priorite; }
    public void setPriorite(int priorite) { this.priorite = priorite; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public LocalDateTime getDateConsultation() { return dateConsultation; }
    public void setDateConsultation(LocalDateTime dateConsultation) { this.dateConsultation = dateConsultation; }

    public Integer getTensionSystolique() { return tensionSystolique; }
    public void setTensionSystolique(Integer tensionSystolique) { this.tensionSystolique = tensionSystolique; }

    public Integer getTensionDiastolique() { return tensionDiastolique; }
    public void setTensionDiastolique(Integer tensionDiastolique) { this.tensionDiastolique = tensionDiastolique; }

    public Double getGlycemie() { return glycemie; }
    public void setGlycemie(Double glycemie) { this.glycemie = glycemie; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public String getPrioriteTexte() {
        return priorite == 1 ? "🔴 URGENT" : "🟢 NORMAL";
    }

    public String getPatientNomComplet() {
        return patientNom + " " + patientPrenom;
    }

    public String getDateConsultationFormatee() {
        return dateConsultation.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public String getTensionFormatee() {
        if (tensionSystolique != null && tensionDiastolique != null) {
            return tensionSystolique + "/" + tensionDiastolique;
        }
        return "-/-";
    }

    public String getGlycemieFormatee() {
        return glycemie != null ? String.valueOf(glycemie) : "-";
    }


}