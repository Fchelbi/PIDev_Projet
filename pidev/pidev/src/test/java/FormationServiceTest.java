import entities.Formation;
import org.junit.jupiter.api.*;
import services.FormationService;

import java.sql.SQLException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FormationServiceTest {

    static FormationService fs;
    static int idFormation;

    @BeforeAll
    static void setUp() {
        fs = new FormationService();
    }

    @Test
    @Order(1)
    void testAjouterFormation() throws SQLException {
        // 1. Create formation
        Formation f = new Formation(0,
                "Boost Your Confidence",
                "Learn how to overcome imposter syndrome",
                "confidence_masterclass.mp4",
                "Self-Improvement");

        // 2. Add it
        fs.insertOne(f);

        // 3. Get all formations
        List<Formation> formations = fs.selectALL();

        // 4. Check if it was added (fix: match the ACTUAL title you inserted)
        assertTrue(formations.stream()
                .anyMatch(form -> form.getTitle().equals("Boost Your Confidence")));

        // 5. Save ID for next tests
        idFormation = formations.get(formations.size() - 1).getId();
        System.out.println("ID Retrieved: " + idFormation);
    }

    @Test
    @Order(2)
    void testModifierFormation() throws SQLException {
        Formation f = new Formation();
        f.setId(idFormation);
        f.setTitle("Updated Confidence");
        f.setDescription("Better Description");
        f.setVideoUrl("new.mp4");
        f.setCategory("Communication");

        fs.updateOne(f);

        List<Formation> formations = fs.selectALL();
        assertTrue(formations.stream()
                .anyMatch(form -> form.getTitle().equals("Updated Confidence")));

        System.out.println("Update successful!");
    }

    @Test
    @Order(3)
    void testSupprimerFormation() throws SQLException {
        Formation f = new Formation();
        f.setId(idFormation);

        fs.deleteOne(f);

        List<Formation> formations = fs.selectALL();
        assertFalse(formations.stream()
                .anyMatch(form -> form.getId() == idFormation));

        System.out.println("Delete successful!");
    }

    @Test
    @Order(4)
    void testAfficherFormations() throws SQLException {
        List<Formation> formations = fs.selectALL();

        assertNotNull(formations);
        System.out.println("All formations:");
        formations.forEach(System.out::println);
    }
}