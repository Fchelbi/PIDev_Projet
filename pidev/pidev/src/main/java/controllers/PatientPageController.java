package controllers;

import entities.User;

/**
 * Interface implemented by all patient sub-page controllers
 * so PatientHome.loadSubPage() can inject the current user.
 *
 * Named PatientPageController to avoid conflict with the existing
 * PatientController class (used by PatientDashboard.fxml).
 */
public interface PatientPageController {
    void setUser(User user);
}