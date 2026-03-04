import entities.User;
import services.serviceUser;
import org.junit.jupiter.api.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceUserTest {

    static serviceUser us;
    static int idUserTest;

    @BeforeAll
    static void setUp() {
        us = new serviceUser();
    }

    @Test
    @Order(1)
    void testAjouterUser() throws SQLException {
        // ✅ FIX: "Client" n'existe pas comme rôle → remplacé par "PATIENT"
        User u = new User(0, "TestNom", "TestPrenom", "test.junit@esprit.tn", "pass123", "PATIENT", "11111111");

        us.insertOne(u);

        List<User> users = us.selectALL();

        Optional<User> userAjoute = users.stream()
                .filter(user -> user.getEmail().equals("test.junit@esprit.tn"))
                .findFirst();

        assertTrue(userAjoute.isPresent(), "Erreur: User non ajouté !");

        idUserTest = userAjoute.get().getId_user();
        System.out.println("Test 1 OK : User ajouté avec ID = " + idUserTest);
    }

    @Test
    @Order(2)
    void testModifierUser() throws SQLException {
        User u = new User();
        u.setId_user(idUserTest);
        u.setNom("NomModifie");
        u.setPrenom("PrenomModifie");
        u.setEmail("test.junit@esprit.tn");
        u.setMdp("passChanged");
        u.setRole("ADMIN"); // ✅ FIX: "Admin" → "ADMIN" (cohérent avec les rôles du système)
        u.setNum_tel("22222222");

        us.updateOne(u);

        List<User> users = us.selectALL();

        boolean isModified = users.stream()
                .anyMatch(user -> user.getId_user() == idUserTest && user.getNom().equals("NomModifie"));

        assertTrue(isModified, "Erreur: Le nom n'a pas été modifié !");
        System.out.println("Test 2 OK : User ID " + idUserTest + " modifié.");
    }

    @Test
    @Order(3)
    void testSupprimerUser() throws SQLException {
        User u = new User();
        u.setId_user(idUserTest);

        us.deleteOne(u);

        List<User> users = us.selectALL();

        boolean exists = users.stream()
                .anyMatch(user -> user.getId_user() == idUserTest);

        assertFalse(exists, "Erreur: Le user n'a pas été supprimé !");
        System.out.println("Test 3 OK : User ID " + idUserTest + " supprimé.");
    }
}