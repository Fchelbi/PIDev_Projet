package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDBConnexion {

    private static final String USERNAME = "root";
    private static final String PASSWORD = "root";   // ✅ une seule déclaration
    private static final String URL = "jdbc:mysql://localhost:3306/project";

    private Connection cnx;
    private static MyDBConnexion instance;

    private MyDBConnexion() {
        try {
            cnx = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("Connexion etablie!");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public static MyDBConnexion getInstance() {
        if (instance == null) instance = new MyDBConnexion();
        return instance;
    }

    public Connection getCnx() {
        return cnx;
    }
}