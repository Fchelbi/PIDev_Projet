package controllers;

import entities.User;

/**
 * Interface commune pour tous les contrôleurs de pages patient.
 * PatientHome.loadSubPage() appelle setUser() via cette interface.
 */
public interface PatientController {
    void setUser(User user);
}
