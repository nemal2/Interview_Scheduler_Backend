// NotificationService.java
package com.nemal.service;

import com.nemal.entity.InterviewRequest;
import com.nemal.entity.Notification;
import com.nemal.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' h:mm a");

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Send notification when interview is scheduled (auto-accepted)
     */
    public void sendInterviewScheduledNotification(InterviewRequest request) {
        String formattedDateTime = request.getPreferredStartDateTime().format(DATE_FORMATTER);

        Notification notification = Notification.builder()
                .recipient(request.getAssignedInterviewer())
                .subject("Interview Scheduled")
                .message(String.format(
                        "An interview has been scheduled for you with candidate %s on %s. " +
                                "Position: %s. Please check your schedule.",
                        request.getCandidateName(),
                        formattedDateTime,
                        request.getCandidateDesignation() != null
                                ? request.getCandidateDesignation().getName()
                                : "Not specified"
                ))
                .type("INTERVIEW_SCHEDULED")
                .relatedEntityId(request.getId())
                .relatedEntityType("INTERVIEW_REQUEST")
                .read(false)
                .build();

        notificationRepository.save(notification);
    }

    /**
     * Send notification when interview is cancelled
     */
    public void sendInterviewCancelledNotification(InterviewRequest request) {
        String formattedDateTime = request.getPreferredStartDateTime().format(DATE_FORMATTER);

        Notification notification = Notification.builder()
                .recipient(request.getAssignedInterviewer())
                .subject("Interview Cancelled")
                .message(String.format(
                        "The interview with candidate %s scheduled for %s has been cancelled by HR.",
                        request.getCandidateName(),
                        formattedDateTime
                ))
                .type("INTERVIEW_CANCELLED")
                .relatedEntityId(request.getId())
                .relatedEntityType("INTERVIEW_REQUEST")
                .read(false)
                .build();

        notificationRepository.save(notification);
    }

    /**
     * Send reminder notification for upcoming interview
     */
    public void sendInterviewReminderNotification(InterviewRequest request) {
        String formattedDateTime = request.getPreferredStartDateTime().format(DATE_FORMATTER);

        Notification notification = Notification.builder()
                .recipient(request.getAssignedInterviewer())
                .subject("Interview Reminder")
                .message(String.format(
                        "Reminder: You have an interview with %s scheduled for %s.",
                        request.getCandidateName(),
                        formattedDateTime
                ))
                .type("INTERVIEW_REMINDER")
                .relatedEntityId(request.getId())
                .relatedEntityType("INTERVIEW_REQUEST")
                .read(false)
                .build();

        notificationRepository.save(notification);
    }
}