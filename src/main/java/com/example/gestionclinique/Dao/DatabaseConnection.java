package com.example.gestionclinique.Dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/clinique_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");  
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL non trouvé !", e);
        }
    }

    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            System.out.println("Connexion réussie à la base clinique_db !");
            System.out.println("Base de données : " + conn.getCatalog());
        } catch (SQLException e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }
}