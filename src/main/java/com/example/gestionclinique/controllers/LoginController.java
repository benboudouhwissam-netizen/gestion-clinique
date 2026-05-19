package com.example.gestionclinique.controllers;

import com.example.gestionclinique.Dao.DatabaseConnection;
import com.example.gestionclinique.utils.SessionUtilisateur;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.*;

public class LoginController {
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtMotDePasse;
    @FXML private ComboBox<String> cmbRole;
    @FXML private Label lblErreur;

    private int userId;
    private String userRole;

    @FXML
    public void initialize() {
        cmbRole.getItems().addAll("medecin", "infirmier", "secretaire");
        cmbRole.getSelectionModel().selectFirst();
    }

    @FXML
    private void authentifier() {
        String email = txtEmail.getText();
        String password = txtMotDePasse.getText();
        String role = cmbRole.getValue();

        if (email.isEmpty() || password.isEmpty()) {
            lblErreur.setText("Veuillez remplir tous les champs.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT id, nom, prenom FROM utilisateurs WHERE email = ? AND mot_de_passe = ? AND role = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            pstmt.setString(3, role);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                userId = rs.getInt("id");
                String nom = rs.getString("nom");
                String prenom = rs.getString("prenom");
                userRole = role;

                SessionUtilisateur.setUtilisateur(userId, nom + " " + prenom, userRole);

                // Redirection vers l'accueil
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/gestionclinique/accueil.fxml"));
                Scene scene = new Scene(loader.load(), 900, 600);
                AccueilController accueilCtrl = loader.getController();
                accueilCtrl.setUtilisateur(userId, nom + " " + prenom, userRole);

                Stage stage = (Stage) txtEmail.getScene().getWindow();
                stage.setScene(scene);
                stage.setTitle("Accueil - Clinique");
            } else {
                lblErreur.setText("Email, mot de passe ou rôle incorrect.");
            }
        } catch (Exception e) {
            lblErreur.setText("Erreur de connexion : " + e.getMessage());
        }
    }

    @FXML
    private void quitter() {
        System.exit(0);
    }
}