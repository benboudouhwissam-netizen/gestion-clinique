package com.example.gestionclinique.controllers;

import com.example.gestionclinique.Dao.DatabaseConnection;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.*;

public class InfirmierController {
    @FXML private Label lblPatient;
    @FXML private TextField txtTensionSyst, txtTensionDiast, txtGlycemie, txtTemperature;
    @FXML private CheckBox chkUrgent;
    @FXML private TextArea txtDiagnostic; // lecture seule

    private int patientId;
    private int infirmierId;

    public void setPatient(int id, String nom, int infirmierId) {
        this.patientId = id;
        this.infirmierId = infirmierId;
        lblPatient.setText("Patient : " + nom);
        chargerDiagnostic();
    }

    private void chargerDiagnostic() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT diagnostic FROM dossiers_medicaux WHERE patient_id = ? ORDER BY date_mise_a_jour DESC LIMIT 1";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, patientId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                txtDiagnostic.setText(rs.getString("diagnostic"));
            } else {
                txtDiagnostic.setText("Aucun diagnostic enregistré.");
            }
        } catch (SQLException e) {
            txtDiagnostic.setText("Erreur de chargement.");
        }
    }

    @FXML
    private void enregistrerConstantes() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "INSERT INTO consultations (patient_id, tension_systolique, tension_diastolique, glycemie, temperature, statut) VALUES (?, ?, ?, ?, ?, 'EN_ATTENTE')";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, patientId);
            pstmt.setString(2, txtTensionSyst.getText().isEmpty() ? null : txtTensionSyst.getText());
            pstmt.setString(3, txtTensionDiast.getText().isEmpty() ? null : txtTensionDiast.getText());
            pstmt.setString(4, txtGlycemie.getText().isEmpty() ? null : txtGlycemie.getText());
            pstmt.setString(5, txtTemperature.getText().isEmpty() ? null : txtTemperature.getText());
            pstmt.executeUpdate();

            // Mise à jour du statut urgent du patient
            if (chkUrgent.isSelected()) {
                PreparedStatement pstmt2 = conn.prepareStatement("UPDATE patients SET est_urgent = TRUE WHERE id = ?");
                pstmt2.setInt(1, patientId);
                pstmt2.executeUpdate();
            }
            showAlert("Succès", "Constantes enregistrées.", Alert.AlertType.INFORMATION);
        } catch (SQLException e) {
            showAlert("Erreur", "Échec : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void retourAccueil() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/gestionclinique/accueil.fxml"));
        Scene scene = new Scene(loader.load(), 900, 600);
        Stage stage = (Stage) lblPatient.getScene().getWindow();
        stage.setScene(scene);
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}