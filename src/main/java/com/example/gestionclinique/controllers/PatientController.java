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
        chargerDocteurs();
        chargerInfirmieres();

        chkUrgent.selectedProperty().addListener((ObservableValue<? extends Boolean> obs, Boolean oldVal, Boolean newVal) -> {
            if (newVal) {
                showAlert("⚠️ URGENT", "Ce patient sera marqué comme CAS URGENT", Alert.AlertType.WARNING);
            }
        });
    }

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
        if (txtNom.getText().isEmpty() || txtPrenom.getText().isEmpty()) {
            showAlert("Erreur", "Le nom et prénom sont obligatoires !", Alert.AlertType.ERROR);
            return;
        }

        progressIndicator.setVisible(true);

        Task<Boolean> sauvegardeTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    if (patientIdEdition > 0) {
                        // ---------- MODIFICATION ----------
                        String sql = "UPDATE patients SET nom=?, prenom=?, date_naissance=?, telephone=?, adresse=?, antecedents=?, est_urgent=? WHERE id=?";
                        PreparedStatement pstmt = conn.prepareStatement(sql);
                        pstmt.setString(1, txtNom.getText());
                        pstmt.setString(2, txtPrenom.getText());
                        pstmt.setObject(3, dpDateNaissance.getValue() != null ? Date.valueOf(dpDateNaissance.getValue()) : null);
                        pstmt.setString(4, txtTelephone.getText());
                        pstmt.setString(5, txtAdresse.getText());
                        pstmt.setString(6, txtAntecedents.getText());
                        pstmt.setBoolean(7, chkUrgent.isSelected());
                        pstmt.setInt(8, patientIdEdition);
                        return pstmt.executeUpdate() > 0;
                    } else {
                        // ---------- CREATION ----------
                        conn.setAutoCommit(false);
                        String sqlPatient = "INSERT INTO patients (nom, prenom, date_naissance, telephone, adresse, antecedents, est_urgent) VALUES (?,?,?,?,?,?,?)";
                        PreparedStatement pstmtPatient = conn.prepareStatement(sqlPatient, Statement.RETURN_GENERATED_KEYS);
                        pstmtPatient.setString(1, txtNom.getText());
                        pstmtPatient.setString(2, txtPrenom.getText());
                        pstmtPatient.setObject(3, dpDateNaissance.getValue() != null ? Date.valueOf(dpDateNaissance.getValue()) : null);
                        pstmtPatient.setString(4, txtTelephone.getText());
                        pstmtPatient.setString(5, txtAdresse.getText());
                        pstmtPatient.setString(6, txtAntecedents.getText());
                        pstmtPatient.setBoolean(7, chkUrgent.isSelected());

                        int affected = pstmtPatient.executeUpdate();
                        if (affected == 0) return false;
                        ResultSet keys = pstmtPatient.getGeneratedKeys();
                        if (!keys.next()) return false;
                        int newId = keys.getInt(1);

                        String sqlConsult = "INSERT INTO consultations (patient_id, tension_systolique, tension_diastolique, glycemie, temperature) VALUES (?,?,?,?,?)";
                        PreparedStatement pstmtConsult = conn.prepareStatement(sqlConsult);
                        pstmtConsult.setInt(1, newId);
                        pstmtConsult.setString(2, txtTensionSyst.getText().isEmpty() ? null : txtTensionSyst.getText());
                        pstmtConsult.setString(3, txtTensionDiast.getText().isEmpty() ? null : txtTensionDiast.getText());
                        pstmtConsult.setString(4, txtGlycemie.getText().isEmpty() ? null : txtGlycemie.getText());
                        pstmtConsult.setString(5, txtTemperature.getText().isEmpty() ? null : txtTemperature.getText());
                        pstmtConsult.executeUpdate();

                        conn.commit();
                        return true;
                    }
                }
            }
        };

        sauvegardeTask.setOnSucceeded(e -> {
            progressIndicator.setVisible(false);
            if (sauvegardeTask.getValue()) {
                if (patientIdEdition > 0) {
                    showAlert("Succès", "Patient modifié avec succès !", Alert.AlertType.INFORMATION);
                    // Retour à l'accueil après modification
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/gestionclinique/accueil.fxml"));
                        Scene scene = new Scene(loader.load(), 900, 600);
                        Stage stage = (Stage) txtNom.getScene().getWindow();
                        stage.setScene(scene);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    String message = chkUrgent.isSelected() ? "⚠️ Patient URGENT enregistré !" : "Patient enregistré !";
                    showAlert("Succès", message, Alert.AlertType.INFORMATION);
                    reinitialiserFormulaire();
                }
            }
        });

        sauvegardeTask.setOnFailed(e -> {
            progressIndicator.setVisible(false);
            showAlert("Erreur", "Échec : " + sauvegardeTask.getException().getMessage(), Alert.AlertType.ERROR);
        });

        new Thread(sauvegardeTask).start();
    }
    private int patientIdEdition = -1;

    private void chargerPatientPourEdition() {
        if (patientIdEdition <= 0) return;
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM patients WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, patientIdEdition);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                txtNom.setText(rs.getString("nom"));
                txtPrenom.setText(rs.getString("prenom"));
                if (rs.getDate("date_naissance") != null)
                    dpDateNaissance.setValue(rs.getDate("date_naissance").toLocalDate());
                txtTelephone.setText(rs.getString("telephone"));
                txtAdresse.setText(rs.getString("adresse"));
                txtAntecedents.setText(rs.getString("antecedents"));
                chkUrgent.setSelected(rs.getBoolean("est_urgent"));
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger le patient : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void setPatientEnEdition(int id) {
        this.patientIdEdition = id;
        chargerPatientPourEdition();   
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
        patientIdEdition = -1;
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