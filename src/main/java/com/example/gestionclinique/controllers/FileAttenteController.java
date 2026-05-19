package com.example.gestionclinique.controllers;

import com.example.gestionclinique.Dao.DatabaseConnection;
import com.example.gestionclinique.models.Consultation;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.*;
import java.time.format.DateTimeFormatter;

public class FileAttenteController {

    @FXML private TableView<Consultation> tableViewConsultations;
    @FXML private Label lblStatistiques;

    private ObservableList<Consultation> consultationsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        chargerConsultations();
    }

    private void chargerConsultations() {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                String sql = "SELECT c.*, p.nom, p.prenom, p.est_urgent " +
                        "FROM consultations c " +
                        "JOIN patients p ON c.patient_id = p.id " +
                        "WHERE c.statut = 'EN_ATTENTE' OR c.statut IS NULL " +
                        "ORDER BY p.est_urgent DESC, c.date_consultation ASC";

                try (Connection conn = DatabaseConnection.getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {

                    consultationsList.clear();
                    while (rs.next()) {
                        Consultation consultation = new Consultation();
                        consultation.setId(rs.getInt("c.id"));
                        consultation.setPatientId(rs.getInt("patient_id"));
                        consultation.setPatientNom(rs.getString("nom"));
                        consultation.setPatientPrenom(rs.getString("prenom"));
                        consultation.setPriorite(rs.getBoolean("est_urgent") ? 1 : 2);

                        String statut = rs.getString("statut");
                        consultation.setStatut(statut != null ? statut : "EN_ATTENTE");

                        consultation.setDateConsultation(rs.getTimestamp("date_consultation").toLocalDateTime());

                        if (rs.getObject("tension_systolique") != null) {
                            consultation.setTensionSystolique(rs.getInt("tension_systolique"));
                        }
                        if (rs.getObject("tension_diastolique") != null) {
                            consultation.setTensionDiastolique(rs.getInt("tension_diastolique"));
                        }
                        if (rs.getObject("glycemie") != null) {
                            consultation.setGlycemie(rs.getDouble("glycemie"));
                        }
                        if (rs.getObject("temperature") != null) {
                            consultation.setTemperature(rs.getDouble("temperature"));
                        }

                        consultationsList.add(consultation);
                    }
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            tableViewConsultations.setItems(consultationsList);
            mettreAJourStatistiques();
        });

        new Thread(task).start();
    }

    @FXML
    private void rafraichirListe() {
        chargerConsultations();
    }

    @FXML
    private void terminerConsultation() {
        Consultation selected = tableViewConsultations.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Aucune sélection", "Veuillez sélectionner une consultation", Alert.AlertType.WARNING);
            return;
        }

        boolean urgentAvant = false;
        for (Consultation c : consultationsList) {
            if (c.getId() == selected.getId()) break;
            if (c.getPriorite() == 1) { // urgent
                urgentAvant = true;
                break;
            }
        }

        if (urgentAvant) {
            showAlert("Priorité urgente", "Des patients URGENTS sont en attente avant celui-ci. Veuillez les traiter d'abord.", Alert.AlertType.ERROR);
            return;
        }

        // Si le patient sélectionné n'est pas urgent, mais qu'il y a d'autres urgents après ? Non, on autorise car les urgents sont déjà passés ?
        // En fait, la liste est triée par priorité décroissante (urgents d'abord). Si on arrive à un patient non urgent, normalement il n'y a plus d'urgents après.
        // On peut aussi vérifier si le patient sélectionné est non urgent et qu'il reste des urgents dans la liste (au cas où le tri ne serait pas parfait).
        long remainingUrgents = consultationsList.stream()
                .filter(c -> c.getPriorite() == 1 && c.getStatut().equals("EN_ATTENTE"))
                .count();
        if (selected.getPriorite() != 1 && remainingUrgents > 0) {
            showAlert("Priorité urgente", "Il reste des patients URGENTS dans la file d'attente. Veuillez d'abord les consulter.", Alert.AlertType.ERROR);
            return;
        }

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("UPDATE consultations SET statut = 'TERMINEE' WHERE id = ?")) {
                    pstmt.setInt(1, selected.getId());
                    pstmt.executeUpdate();
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            showAlert("Succès", "Consultation terminée", Alert.AlertType.INFORMATION);
            chargerConsultations();
        });

        new Thread(task).start();
    }

    @FXML
    private void nouveauPatient() throws Exception {
        Stage stage = (Stage) tableViewConsultations.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/gestionclinique/patient_form.fxml"));
        stage.setScene(new Scene(loader.load(), 900, 700));
        stage.setTitle("Nouveau Patient");
    }

    @FXML
    private void voirTableauBord() {
        long urgents = consultationsList.stream().filter(c -> c.getPriorite() == 1).count();
        long normaux = consultationsList.stream().filter(c -> c.getPriorite() == 2).count();
        showAlert("Tableau de bord",
                "🔴 Urgents: " + urgents + "\n" +
                        "🟢 Normaux: " + normaux + "\n" +
                        "📋 Total: " + consultationsList.size(),
                Alert.AlertType.INFORMATION);
    }

    private void mettreAJourStatistiques() {
        long urgents = consultationsList.stream().filter(c -> c.getPriorite() == 1).count();
        long normaux = consultationsList.stream().filter(c -> c.getPriorite() == 2).count();
        lblStatistiques.setText("📊 File: " + consultationsList.size() + " | 🔴 Urgents: " + urgents + " | 🟢 Normaux: " + normaux);
    }

    @FXML
    private void retourAccueil() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/gestionclinique/accueil.fxml"));
        Scene scene = new Scene(loader.load(), 900, 600);
        Stage stage = (Stage) lblStatistiques.getScene().getWindow();
        stage.setScene(scene);
        stage.setTitle("Accueil - Clinique");
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}