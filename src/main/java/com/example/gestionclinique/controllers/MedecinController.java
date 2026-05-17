package com.example.gestionclinique.controllers;

import com.example.gestionclinique.Dao.DatabaseConnection;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.*;
import java.time.format.DateTimeFormatter;

public class MedecinController {
    @FXML private Label lblPatient;
    @FXML private Label lblDateConsult;
    @FXML private Label lblTension;
    @FXML private Label lblGlycemie;
    @FXML private Label lblTemperature;
    @FXML private TextArea txtRadios, txtScanner, txtAnalyses, txtDiagnostic;

    private int patientId;
    private int medecinId;
    private int dossierId = -1;

    private void chargerConstantes(int patientId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT tension_systolique, tension_diastolique, glycemie, temperature, date_consultation " +
                    "FROM consultations WHERE patient_id = ? ORDER BY date_consultation DESC LIMIT 1";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, patientId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int sys = rs.getInt("tension_systolique");
                int dia = rs.getInt("tension_diastolique");
                lblTension.setText((sys > 0 ? sys : "?") + "/" + (dia > 0 ? dia : "?"));
                lblGlycemie.setText(rs.getObject("glycemie") != null ? rs.getDouble("glycemie") + " g/L" : "non renseignée");
                lblTemperature.setText(rs.getObject("temperature") != null ? rs.getDouble("temperature") + " °C" : "non renseignée");

                Timestamp ts = rs.getTimestamp("date_consultation");
                if (ts != null) {
                    lblDateConsult.setText(ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                } else {
                    lblDateConsult.setText("inconnue");
                }
            } else {
                lblTension.setText("aucune donnée");
                lblGlycemie.setText("aucune donnée");
                lblTemperature.setText("aucune donnée");
                lblDateConsult.setText("aucune consultation");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            lblTension.setText("erreur");
        }
    }

    public void setPatient(int id, String nom, int medecinId) {
        this.patientId = id;
        this.medecinId = medecinId;
        lblPatient.setText("Patient : " + nom);
        chargerDossier();
        chargerConstantes(id);
    }

    private void chargerDossier() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM dossiers_medicaux WHERE patient_id = ? AND medecin_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, patientId);
            pstmt.setInt(2, medecinId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                dossierId = rs.getInt("id");
                txtRadios.setText(rs.getString("radios"));
                txtScanner.setText(rs.getString("scanner"));
                txtAnalyses.setText(rs.getString("analyses"));
                txtDiagnostic.setText(rs.getString("diagnostic"));
            } else {
                // Créer un dossier vide
                String insert = "INSERT INTO dossiers_medicaux (patient_id, medecin_id) VALUES (?, ?)";
                PreparedStatement pstmt2 = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
                pstmt2.setInt(1, patientId);
                pstmt2.setInt(2, medecinId);
                pstmt2.executeUpdate();
                ResultSet keys = pstmt2.getGeneratedKeys();
                if (keys.next()) dossierId = keys.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void enregistrerDossier() {
        if (dossierId == -1) return;
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE dossiers_medicaux SET radios = ?, scanner = ?, analyses = ?, diagnostic = ? WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, txtRadios.getText());
            pstmt.setString(2, txtScanner.getText());
            pstmt.setString(3, txtAnalyses.getText());
            pstmt.setString(4, txtDiagnostic.getText());
            pstmt.setInt(5, dossierId);
            pstmt.executeUpdate();
            showAlert("Succès", "Dossier médical mis à jour.", Alert.AlertType.INFORMATION);
        } catch (SQLException e) {
            showAlert("Erreur", "Sauvegarde échouée : " + e.getMessage(), Alert.AlertType.ERROR);
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