package com.example.gestionclinique.controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.beans.value.ObservableValue;

import com.example.gestionclinique.Dao.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;

public class PatientController {

    @FXML private TextField txtNom, txtPrenom, txtTelephone, txtAdresse;
    @FXML private TextField txtTensionSyst, txtTensionDiast, txtGlycemie, txtTemperature;
    @FXML private TextArea txtAntecedents;
    @FXML private DatePicker dpDateNaissance;
    @FXML private CheckBox chkUrgent;
    @FXML private ComboBox<String> cmbDocteur, cmbInfirmiere;
    @FXML private ProgressIndicator progressIndicator;

    @FXML
    public void initialize() {
        // Charger les données au démarrage
        chargerDocteurs();
        chargerInfirmieres();

        // Alerte pour cas urgent (CORRIGÉ)
        chkUrgent.selectedProperty().addListener((ObservableValue<? extends Boolean> obs, Boolean oldVal, Boolean newVal) -> {
            if (newVal) {
                showAlert("⚠️ URGENT", "Ce patient sera marqué comme CAS URGENT", Alert.AlertType.WARNING);
            }
        });
    }

    // CORRIGÉ : Charger les docteurs depuis la base
    private void chargerDocteurs() {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                String sql = "SELECT nom, specialite FROM docteurs";
                try (Connection conn = DatabaseConnection.getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        String docteur = rs.getString("nom") + " - " + rs.getString("specialite");
                        Platform.runLater(() -> cmbDocteur.getItems().add(docteur));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    // CORRIGÉ : Charger les infirmières depuis la base
    private void chargerInfirmieres() {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                String sql = "SELECT nom FROM infirmieres";
                try (Connection conn = DatabaseConnection.getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        String infirmiere = rs.getString("nom");
                        Platform.runLater(() -> cmbInfirmiere.getItems().add(infirmiere));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    @FXML
    private void enregistrerPatient() {
        // Validation des champs obligatoires
        if (txtNom.getText().isEmpty() || txtPrenom.getText().isEmpty()) {
            showAlert("Erreur", "Le nom et prénom sont obligatoires !", Alert.AlertType.ERROR);
            return;
        }

        progressIndicator.setVisible(true);

        Task<Boolean> sauvegardeTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    conn.setAutoCommit(false);

                    // 1. Insérer le patient
                    String sqlPatient = "INSERT INTO patients (nom, prenom, date_naissance, telephone, adresse, antecedents, est_urgent) VALUES (?, ?, ?, ?, ?, ?, ?)";
                    PreparedStatement pstmtPatient = conn.prepareStatement(sqlPatient, Statement.RETURN_GENERATED_KEYS);

                    pstmtPatient.setString(1, txtNom.getText());
                    pstmtPatient.setString(2, txtPrenom.getText());

                    if (dpDateNaissance.getValue() != null) {
                        pstmtPatient.setDate(3, Date.valueOf(dpDateNaissance.getValue()));
                    } else {
                        pstmtPatient.setNull(3, Types.DATE);
                    }

                    pstmtPatient.setString(4, txtTelephone.getText());
                    pstmtPatient.setString(5, txtAdresse.getText());
                    pstmtPatient.setString(6, txtAntecedents.getText());
                    pstmtPatient.setBoolean(7, chkUrgent.isSelected());

                    int affectedRows = pstmtPatient.executeUpdate();
                    if (affectedRows == 0) return false;

                    ResultSet generatedKeys = pstmtPatient.getGeneratedKeys();
                    if (!generatedKeys.next()) return false;
                    int patientId = generatedKeys.getInt(1);

                    // 2. Insérer la consultation
                    String sqlConsultation = "INSERT INTO consultations (patient_id, tension_systolique, tension_diastolique, glycemie, temperature) VALUES (?, ?, ?, ?, ?)";
                    PreparedStatement pstmtConsult = conn.prepareStatement(sqlConsultation);
                    pstmtConsult.setInt(1, patientId);

                    pstmtConsult.setString(2, txtTensionSyst.getText().isEmpty() ? null : txtTensionSyst.getText());
                    pstmtConsult.setString(3, txtTensionDiast.getText().isEmpty() ? null : txtTensionDiast.getText());
                    pstmtConsult.setString(4, txtGlycemie.getText().isEmpty() ? null : txtGlycemie.getText());
                    pstmtConsult.setString(5, txtTemperature.getText().isEmpty() ? null : txtTemperature.getText());

                    pstmtConsult.executeUpdate();

                    conn.commit();
                    return true;

                } catch (SQLException e) {
                    throw new Exception("Erreur base de données : " + e.getMessage());
                }
            }
        };

        sauvegardeTask.setOnSucceeded(e -> {
            progressIndicator.setVisible(false);
            if (sauvegardeTask.getValue()) {
                String message = chkUrgent.isSelected() ?
                        "⚠️ Patient URGENT enregistré avec succès !" :
                        "Patient enregistré avec succès !";
                showAlert("Succès", message, Alert.AlertType.INFORMATION);
                reinitialiserFormulaire();
            }
        });

        sauvegardeTask.setOnFailed(e -> {
            progressIndicator.setVisible(false);
            showAlert("Erreur", "Échec : " + sauvegardeTask.getException().getMessage(), Alert.AlertType.ERROR);
        });

        new Thread(sauvegardeTask).start();
    }

    @FXML
    private void reinitialiserFormulaire() {
        txtNom.clear();
        txtPrenom.clear();
        txtTelephone.clear();
        txtAdresse.clear();
        txtAntecedents.clear();
        txtTensionSyst.clear();
        txtTensionDiast.clear();
        txtGlycemie.clear();
        txtTemperature.clear();
        dpDateNaissance.setValue(null);
        chkUrgent.setSelected(false);
        if (cmbDocteur != null) cmbDocteur.getSelectionModel().clearSelection();
        if (cmbInfirmiere != null) cmbInfirmiere.getSelectionModel().clearSelection();
    }

    @FXML
    private void quitterApplication() {
        Platform.exit();
    }

    @FXML
    private void voirFileAttente() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/gestionclinique/file_attente.fxml"));
        Scene scene = new Scene(loader.load(), 1000, 700);

        Stage stage = (Stage) txtNom.getScene().getWindow();
        stage.setScene(scene);
        stage.setTitle("📋 File d'attente - Priorité patients urgents");
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}