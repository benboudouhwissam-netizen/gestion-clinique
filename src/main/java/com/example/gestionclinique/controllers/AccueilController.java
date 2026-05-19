package com.example.gestionclinique.controllers;

import com.example.gestionclinique.Dao.DatabaseConnection;
import javafx.collections.FXCollections;
import com.example.gestionclinique.utils.SessionUtilisateur;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class AccueilController {
    @FXML private Label lblBienvenue;
    @FXML private ComboBox<String> cmbPatients;
    @FXML private ComboBox<String> cmbActions;
    @FXML private Label lblStatut;

    private int utilisateurId;
    private String utilisateurNom;
    private String role;
    private Map<String, Integer> patientsMap = new HashMap<>();
    private boolean listenerAdded = false;

    @FXML
    public void initialize() {
        int id = SessionUtilisateur.getId();
        String nom = SessionUtilisateur.getNom();
        String roleSession = SessionUtilisateur.getRole();
        if (roleSession != null) {
            setUtilisateur(id, nom, roleSession);
        }
    }

    public void setUtilisateur(int id, String nom, String role) {
        this.utilisateurId = id;
        this.utilisateurNom = nom;
        this.role = role;
        lblBienvenue.setText("Bienvenue " + role.toUpperCase() + " " + nom);
        chargerPatients();
        chargerActions();

        if (!listenerAdded) {
            cmbActions.valueProperty().addListener((obs, oldVal, newVal) -> {
                if ("Nouveau patient".equals(newVal)) {
                    cmbPatients.setDisable(true);
                    cmbPatients.setValue(null);
                } else {
                    cmbPatients.setDisable(false);
                    if (cmbPatients.getItems().isEmpty())
                        chargerPatients();
                    else if (cmbPatients.getValue() == null && !cmbPatients.getItems().isEmpty())
                        cmbPatients.getSelectionModel().selectFirst();
                }
            });
            listenerAdded = true;
        }
    }

    private void chargerPatients() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql;
            if ("secretaire".equals(role)) {
                // La secrétaire voit tous les patients (pour modification)
                sql = "SELECT id, nom, prenom FROM patients ORDER BY nom";
            } else {
                // Infirmier et médecin : ne voient que les patients qui n'ont PAS de consultation TERMINEE
                // (ou qui n'ont jamais eu de consultation, ou seulement EN_ATTENTE)
                sql = "SELECT p.id, p.nom, p.prenom FROM patients p " +
                        "LEFT JOIN consultations c ON p.id = c.patient_id AND c.statut = 'TERMINEE' " +
                        "WHERE c.id IS NULL " +
                        "ORDER BY p.nom";
            }
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            cmbPatients.getItems().clear();
            patientsMap.clear();
            while (rs.next()) {
                String nomComplet = rs.getString("nom") + " " + rs.getString("prenom");
                cmbPatients.getItems().add(nomComplet);
                patientsMap.put(nomComplet, rs.getInt("id"));
            }
            if (!cmbPatients.getItems().isEmpty())
                cmbPatients.getSelectionModel().selectFirst();
        } catch (SQLException e) {
            lblStatut.setText("Erreur chargement patients: " + e.getMessage());
        }
    }

    private void chargerActions() {
        cmbActions.getItems().clear();
        switch (role) {
            case "secretaire":
                cmbActions.getItems().addAll("Nouveau patient", "Modifier coordonnées patient");
                break;
            case "infirmier":
                cmbActions.getItems().addAll("Prendre constantes", "Consulter diagnostic");
                break;
            case "medecin":
                cmbActions.getItems().addAll("Gérer dossier médical", "Historique consultations");
                break;
        }
        if (!cmbActions.getItems().isEmpty())
            cmbActions.getSelectionModel().selectFirst();
    }

    private void ouvrirHistorique(int patientId, String patientNom) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/gestionclinique/historique_consultations.fxml"));
        Scene scene = new Scene(loader.load(), 1000, 700);
        HistoriqueController ctrl = loader.getController();
        ctrl.setPatient(patientId, patientNom);
        Stage stage = (Stage) lblBienvenue.getScene().getWindow();
        stage.setScene(scene);
        stage.setTitle("Historique des consultations - " + patientNom);
    }

    private void ouvrirFormulaireModification(int patientId, String patientNom) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/gestionclinique/patient_form.fxml"));
        Scene scene = new Scene(loader.load(), 900, 700);
        PatientController ctrl = loader.getController();
        ctrl.setPatientEnEdition(patientId);
        Stage stage = (Stage) lblBienvenue.getScene().getWindow();
        stage.setScene(scene);
        stage.setTitle("Modifier patient - " + patientNom);
    }

    private boolean verifierUrgentsEnAttente(int patientIdSelectionne, String action) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Récupérer la liste des patients urgents en attente (qui ont une consultation EN_ATTENTE)
            String sql = "SELECT DISTINCT p.id, p.nom, p.prenom " +
                    "FROM patients p " +
                    "JOIN consultations c ON p.id = c.patient_id " +
                    "WHERE p.est_urgent = 1 AND c.statut = 'EN_ATTENTE' AND p.id != ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, patientIdSelectionne);
            ResultSet rs = pstmt.executeQuery();

            StringBuilder urgents = new StringBuilder();
            while (rs.next()) {
                if (urgents.length() > 0) urgents.append(", ");
                urgents.append(rs.getString("prenom")).append(" ").append(rs.getString("nom"));
            }

            if (urgents.length() > 0) {
                showAlert("Priorité urgente",
                        "Impossible de " + action + " car des patients URGENTS sont encore en attente :\n" + urgents.toString() +
                                "\n\nVeuillez d'abord les traiter dans la file d'attente.",
                        Alert.AlertType.WARNING);
                return false;
            }
            return true;
        } catch (SQLException e) {
            showAlert("Erreur", "Vérification des urgents impossible : " + e.getMessage(), Alert.AlertType.ERROR);
            return false;
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void executerAction() {
        String action = cmbActions.getValue();
        if (action == null) {
            lblStatut.setText("Veuillez choisir une action.");
            return;
        }

        if ("Nouveau patient".equals(action)) {
            try {
                ouvrirFormulairePatient();
            } catch (Exception e) {
                lblStatut.setText("Erreur ouverture formulaire : " + e.getMessage());
            }
            return;
        }

        String patient = cmbPatients.getValue();
        if (patient == null) {
            lblStatut.setText("Veuillez sélectionner un patient.");
            return;
        }
        int patientId = patientsMap.get(patient);

        try {
            switch (role) {
                case "infirmier":
                    if ("Prendre constantes".equals(action)) {
                        if (verifierUrgentsEnAttente(patientId, "prendre les constantes")) {
                            ouvrirConsultationInfirmier(patientId, patient);
                        }
                    } else if ("Consulter diagnostic".equals(action)) {
                        // Pour consulter le diagnostic, on peut autoriser sans vérification (lecture seule)
                        ouvrirConsultationInfirmier(patientId, patient);
                    }
                    break;

                case "medecin":
                    if ("Gérer dossier médical".equals(action)) {
                        if (verifierUrgentsEnAttente(patientId, "ouvrir le dossier médical")) {
                            ouvrirDossierMedical(patientId, patient);
                        }
                    } else if ("Historique consultations".equals(action)) {
                        // L'historique ne modifie rien, on autorise
                        ouvrirHistorique(patientId, patient);
                    }
                    break;
                case "secretaire":
                    if ("Modifier coordonnées patient".equals(action)) {
                        ouvrirFormulaireModification(patientId, patient);
                    }
                    break;
                default:
                    lblStatut.setText("Action non reconnue pour ce rôle.");
            }
        } catch (Exception e) {
            lblStatut.setText("Erreur : " + e.getMessage());
        }
    }


    private void ouvrirFormulairePatient() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/gestionclinique/patient_form.fxml"));
        Scene scene = new Scene(loader.load(), 900, 700);
        Stage stage = (Stage) lblBienvenue.getScene().getWindow();
        stage.setScene(scene);
        stage.setTitle("Nouveau patient - Secrétaire");
    }

    private void ouvrirConsultationInfirmier(int patientId, String patientNom) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/gestionclinique/infirmier_consultation.fxml"));
        Scene scene = new Scene(loader.load(), 800, 700);
        InfirmierController ctrl = loader.getController();
        ctrl.setPatient(patientId, patientNom, utilisateurId);
        Stage stage = (Stage) lblBienvenue.getScene().getWindow();
        stage.setScene(scene);
        stage.setTitle("Consultation infirmier");
    }

    private void ouvrirDossierMedical(int patientId, String patientNom) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/gestionclinique/medecin_diagnostic.fxml"));
        Scene scene = new Scene(loader.load(), 900, 700);
        MedecinController ctrl = loader.getController();
        ctrl.setPatient(patientId, patientNom, utilisateurId);
        Stage stage = (Stage) lblBienvenue.getScene().getWindow();
        stage.setScene(scene);
        stage.setTitle("Dossier médical - Médecin");
    }

    @FXML
    private void deconnexion() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/gestionclinique/login.fxml"));
        Scene scene = new Scene(loader.load(), 800, 700);
        Stage stage = (Stage) lblBienvenue.getScene().getWindow();
        stage.setScene(scene);
        stage.setTitle("Connexion - Clinique");
    }
}