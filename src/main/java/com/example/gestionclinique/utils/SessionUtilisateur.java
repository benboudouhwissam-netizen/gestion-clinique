package com.example.gestionclinique.utils;

public class SessionUtilisateur {
    private static int id;
    private static String nom;
    private static String role;

    public static void setUtilisateur(int id, String nom, String role) {
        SessionUtilisateur.id = id;
        SessionUtilisateur.nom = nom;
        SessionUtilisateur.role = role;
    }

    public static int getId() { return id; }
    public static String getNom() { return nom; }
    public static String getRole() { return role; }
}