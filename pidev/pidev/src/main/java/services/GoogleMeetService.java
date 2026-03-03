package services;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;

import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Date;

public class GoogleMeetService {

    private static final String APPLICATION_NAME = "PsyConsultationApp";

    public static Calendar getCalendarService() throws Exception {

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                GsonFactory.getDefaultInstance(),
                new InputStreamReader(
                        GoogleMeetService.class.getResourceAsStream("/credentials.json")
                )
        );

        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        GsonFactory.getDefaultInstance(),
                        clientSecrets,
                        Collections.singleton(CalendarScopes.CALENDAR)
                ).build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();

        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
        ).setApplicationName(APPLICATION_NAME).build();
    }

    public static String createMeeting(String dateTimeString) throws Exception {

        Calendar service = getCalendarService();

        // Example input: "2026-03-25 12:00"
        String startDateTime = dateTimeString.replace(" ", "T") + ":00";
        String endDateTime = addOneHour(startDateTime);

        EventDateTime start = new EventDateTime()
                .setDateTime(new com.google.api.client.util.DateTime(startDateTime + "+01:00"))
                .setTimeZone("Africa/Tunis");

        EventDateTime end = new EventDateTime()
                .setDateTime(new com.google.api.client.util.DateTime(endDateTime + "+01:00"))
                .setTimeZone("Africa/Tunis");

        Event event = new Event()
                .setSummary("Consultation Psychologique")
                .setDescription("Consultation en ligne via Google Meet")
                .setStart(start)
                .setEnd(end);

        ConferenceData conferenceData = new ConferenceData();
        CreateConferenceRequest createConferenceRequest = new CreateConferenceRequest();
        createConferenceRequest.setRequestId("random-" + System.currentTimeMillis());

        conferenceData.setCreateRequest(createConferenceRequest);
        event.setConferenceData(conferenceData);

        Event createdEvent = service.events()
                .insert("primary", event)
                .setConferenceDataVersion(1)
                .execute();

        return createdEvent.getHangoutLink();
    }

    private static String addOneHour(String startDateTime) {
        java.time.LocalDateTime dateTime =
                java.time.LocalDateTime.parse(startDateTime);

        return dateTime.plusHours(1).toString();
    }
}