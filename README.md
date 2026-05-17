# Gestion Clinique - Application JavaFX

## Description
Application complète de gestion clinique avec authentification et gestion des rôles (médecin, infirmier, secrétaire).  
Fonctionnalités :

- **Secrétaire** : enregistrement des patients, gestion des antécédents, modification des coordonnées.
- **Infirmier** : prise des constantes vitales (tension, glycémie, température), visualisation du diagnostic.
- **Médecin** : dossier médical complet (radios, scanner, analyses, diagnostic), historique des consultations.
- **Priorité des patients urgents** : un patient marqué "urgent" passe en tête de file d'attente ; l’infirmier et le médecin ne peuvent pas consulter un patient normal si des urgents sont en attente.
- **File d’attente dynamique** : affichage trié par priorité et par date.
- **Multithreading** : toutes les opérations longues (BDD) sont asynchrones pour une interface fluide.

## Technologies utilisées
- Java 17
- JavaFX (interface graphique)
- MySQL (base de données)
- Maven (gestion des dépendances)
- CSS (stylisation)
- Multithreading (Task JavaFX)

## Installation

### Prérequis
- Java 17
- MySQL Server / XAMPP
- IntelliJ IDEA (recommandé)

### Configuration de la base de données
1. Lancer XAMPP et démarrer MySQL.
2. Créer la base de données `clinique_db`.
3. Exécuter les scripts SQL suivants (dans l’ordre) :

```sql
-- Table patients
CREATE TABLE patients (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nom VARCHAR(50) NOT NULL,
    prenom VARCHAR(50) NOT NULL,
    date_naissance DATE,
    telephone VARCHAR(15),
    adresse TEXT,
    antecedents TEXT,
    est_urgent BOOLEAN DEFAULT FALSE,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table consultations
CREATE TABLE consultations (
    id INT PRIMARY KEY AUTO_INCREMENT,
    patient_id INT NOT NULL,
    tension_systolique INT,
    tension_diastolique INT,
    glycemie DECIMAL(5,2),
    temperature DECIMAL(4,2),
    statut VARCHAR(20) DEFAULT 'EN_ATTENTE',
    date_consultation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);

-- Table utilisateurs (authentification)
CREATE TABLE utilisateurs (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nom VARCHAR(50) NOT NULL,
    prenom VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    mot_de_passe VARCHAR(255) NOT NULL,
    role ENUM('medecin', 'infirmier', 'secretaire') NOT NULL,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table dossiers_medicaux
CREATE TABLE dossiers_medicaux (
    id INT PRIMARY KEY AUTO_INCREMENT,
    patient_id INT NOT NULL,
    medecin_id INT NOT NULL,
    diagnostic TEXT,
    radios TEXT,
    scanner TEXT,
    analyses TEXT,
    date_mise_a_jour TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    FOREIGN KEY (medecin_id) REFERENCES utilisateurs(id)
);

-- Insertion de comptes de test
INSERT INTO utilisateurs (nom, prenom, email, mot_de_passe, role) VALUES
('Dupont', 'Martin', 'medecin@clinique.com', 'mdp123', 'medecin'),
('Durand', 'Julie', 'infirmier@clinique.com', 'mdp123', 'infirmier'),
('Petit', 'Sophie', 'secretaire@clinique.com', 'mdp123', 'secretaire');
