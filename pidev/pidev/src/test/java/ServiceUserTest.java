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
    // Hédhi variable bch nkhabiw fiha l ID mta3 l user elli bch nasn3ouh
    // Bch njarbou bih l modification wel suppression mba3d
    static int idUserTest;

    @BeforeAll
    static void setUp() {
        us = new serviceUser();
    }

    @Test
    @Order(1)
    void testAjouterUser() throws SQLException {
        // 1. Création User de test
        // N.B: L'ID 0 ma yhemmech khater l base ta3tih ID auto-increment
        User u = new User(0, "TestNom", "TestPrenom", "test.junit@esprit.tn", "pass123", "Client", "11111111");

        // 2. Appel Service Ajouter
        us.insertOne(u);

        // 3. Vérification
        List<User> users = us.selectALL();

        // Nlawej 3al user elli zedtou bel email mte3ou (Unique)
        Optional<User> userAjoute = users.stream()
                .filter(user -> user.getEmail().equals("test.junit@esprit.tn"))
                .findFirst();

        // Nthabbet elli howa mawjoud
        assertTrue(userAjoute.isPresent(), "Erreur: User non ajouté !");

        // NEKHO L ID MTE3OU W NKHABBIOUH (Important barcha lel test modification)
        idUserTest = userAjoute.get().getId_user();
        System.out.println("Test 1 OK : User ajouté avec ID = " + idUserTest);
    }

    @Test
    @Order(2)
    void testModifierUser() throws SQLException {
        // 1. Préparer l user b l ID elli khabbineh (idUserTest)
        User u = new User();
        u.setId_user(idUserTest); // <-- Houni l astuce bch test ywali dynamique
        u.setNom("NomModifie");
        u.setPrenom("PrenomModifie");
        u.setEmail("test.junit@esprit.tn"); // Nafs l email
        u.setMdp("passChanged");
        u.setRole("Admin");
        u.setNum_tel("22222222");

        // 2. Appel Service Modifier
        us.updateOne(u);

        // 3. Vérification
        List<User> users = us.selectALL();

        // Nchoufou eske l nom tbaddel walla lé
        boolean isModified = users.stream()
                .anyMatch(user -> user.getId_user() == idUserTest && user.getNom().equals("NomModifie"));

        assertTrue(isModified, "Erreur: Le nom n'a pas été modifié !");
        System.out.println("Test 2 OK : User ID " + idUserTest + " modifié.");
    }

    // Bonus : Test Suppression (Nettoyage)
    @Test
    @Order(3)
    void testSupprimerUser() throws SQLException {
        User u = new User();
        u.setId_user(idUserTest);

        us.deleteOne(u);

        List<User> users = us.selectALL();

        // Nthabtou elli howa ma3adch mawjoud
        boolean exists = users.stream()
                .anyMatch(user -> user.getId_user() == idUserTest);

        assertFalse(exists, "Erreur: Le user n'a pas été supprimé !");
        System.out.println("Test 3 OK : User ID " + idUserTest + " supprimé.");
    }
}