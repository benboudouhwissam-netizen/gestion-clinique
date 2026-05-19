package com.example.gestionclinique.controllers;

import com.example.gestionclinique.Dao.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import java.sql.*;
import java.time.format.DateTimeFormatter;

public class HistoriqueController {

    @FXML private Label lblPatient;
    @FXML private TableView<ConsultationHistorique> tableViewHistorique;

    private int patientId;
    private String patientNom;

    public void setPatient(int id, String nom) {
        this.patientId = id;
        this.patientNom = nom;
        lblPatient.setText("Historique des consultations de : " + nom);
        chargerHistorique();
    }

    private void chargerHistorique() {
        ObservableList<ConsultationHistorique> list = FXCollections.observableArrayList();
        String sql = "SELECT c.date_consultation, c.tension_systolique, c.tension_diastolique, " +
                "c.glycemie, c.temperature, c.statut, " +
                "(SELECT diagnostic FROM dossiers_medicaux WHERE patient_id = c.patient_id ORDER BY date_mise_a_jour DESC LIMIT 1) AS diagnostic " +
                "FROM consultations c " +
                "WHERE c.patient_id = ? " +
                "ORDER BY c.date_consultation DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, patientId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                ConsultationHistorique ch = new ConsultationHistorique();
                Timestamp ts = rs.getTimestamp("date_consultation");
                if (ts != null)
                    ch.setDateConsultationFormatee(ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

                int sys = rs.getInt("tension_systolique");
                int dia = rs.getInt("tension_diastolique");
                ch.setTensionFormatee((sys > 0 ? sys : "?") + "/" + (dia > 0 ? dia : "?"));

                ch.setGlycemieFormatee(rs.getObject("glycemie") != null ? rs.getDouble("glycemie") + " g/L" : "-");
                ch.setTemperatureFormatee(rs.getObject("temperature") != null ? rs.getDouble("temperature") + " °C" : "-");

                String diag = rs.getString("diagnostic");
                ch.setDiagnostic(diag != null ? diag : "Aucun diagnostic");
                ch.setStatut(rs.getString("statut"));
                list.add(ch);
            }
            tableViewHistorique.setItems(list);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger l'historique : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void retourAccueil() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/gestionclinique/accueil.fxml"));
        Scene scene = new Scene(loader.load(), 900, 600);
        Stage stage = (Stage) lblPatient.getScene().getWindow();
        stage.setScene(scene);
        stage.setTitle("Accueil - Clinique");
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public static class ConsultationHistorique {
        private String dateConsultationFormatee;
        private String tensionFormatee;
        private String glycemieFormatee;
        private String temperatureFormatee;
        private String diagnostic;
        private String statut;

        public String getDateConsultationFormatee() { return dateConsultationFormatee; }
        public void setDateConsultationFormatee(String d) { this.dateConsultationFormatee = d; }
        public String getTensionFormatee() { return tensionFormatee; }
        public void setTensionFormatee(String t) { this.tensionFormatee = t; }
        public String getGlycemieFormatee() { return glycemieFormatee; }
        public void setGlycemieFormatee(String g) { this.glycemieFormatee = g; }
        public String getTemperatureFormatee() { return temperatureFormatee; }
        public void setTemperatureFormatee(String temp) { this.temperatureFormatee = temp; }
        public String getDiagnostic() { return diagnostic; }
        public void setDiagnostic(String d) { this.diagnostic = d; }
        public String getStatut() { return statut; }
        public void setStatut(String s) { this.statut = s; }
    }
}