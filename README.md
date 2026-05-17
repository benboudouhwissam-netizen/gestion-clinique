# 🏥 Gestion Clinique - Application JavaFX

## Description
Application de gestion clinique permettant à une secrétaire de :
- Enregistrer les informations des patients
- Gérer les antécédents médicaux
- Enregistrer les constantes vitales (tension, glycémie, température)
- Gérer les cas urgents (priorité en file d'attente)
- Visualiser la file d'attente (patients URGENTS en premier)

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
- IntelliJ IDEA

### Configuration de la base de données
1. Lancer XAMPP et démarrer MySQL
2. Créer la base de données `clinique_db`
3. Exécuter le script SQL suivant :

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

-- Table docteurs et infirmieres
INSERT INTO docteurs (nom, specialite, telephone) VALUES
('Dr. Dupont Martin', 'Cardiologue', '0612345678'),
('Dr. Petit Sophie', 'Généraliste', '0623456789');

INSERT INTO infirmieres (nom, telephone) VALUES
('Durand Marie', '0611111111'),
('Lefevre Julie', '0622222222');
