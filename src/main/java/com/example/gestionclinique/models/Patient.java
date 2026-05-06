package com.example.gestionclinique.models;

import java.time.LocalDate;

public class Patient {
    private int id;
    private String nom;
    private String prenom;
    private LocalDate dateNaissance;
    private String telephone;
    private String adresse;
    private String antecedents;
    private boolean estUrgent;

    // Constructeur
    public Patient() {}

    public Patient(String nom, String prenom, LocalDate dateNaissance,
                   String telephone, String adresse, String antecedents, boolean estUrgent) {
        this.nom = nom;
        this.prenom = prenom;
        this.dateNaissance = dateNaissance;
        this.telephone = telephone;
        this.adresse = adresse;
        this.antecedents = antecedents;
        this.estUrgent = estUrgent;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getAntecedents() { return antecedents; }
    public void setAntecedents(String antecedents) { this.antecedents = antecedents; }

    public boolean isEstUrgent() { return estUrgent; }
    public void setEstUrgent(boolean estUrgent) { this.estUrgent = estUrgent; }
}