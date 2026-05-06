package com.example.gestionclinique;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // ⚠️ Le chemin DOIT commencer par "/" et être exact
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/gestionclinique/patient_form.fxml"));

        Scene scene = new Scene(fxmlLoader.load(), 900, 700);
        stage.setTitle("Gestion Clinique - Formulaire Patient");
        stage.setScene(scene);
        stage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }
}