package tn.esprit.projet.services;

import tn.esprit.projet.entities.User;
import tn.esprit.projet.utils.MyDBConnexion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {
    private Connection cnx;

    public UserService() {
        cnx = MyDBConnexion.getInstance().getCnx();
    }

    public void ajouterUtilisateur(User u) throws SQLException {
        String req = "INSERT INTO utilisateur (nom, prenom, email, mot_de_pass, date_inscription) VALUES (?, ?, ?, ?, NOW())";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setString(3, u.getEmail());
        ps.setString(4, u.getMotDePasse());
        ps.executeUpdate();
    }
}