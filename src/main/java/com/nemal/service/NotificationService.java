// NotificationService.java  
package com.nemal.service;

import com.nemal.entity.InterviewRequest;
import com.nemal.entity.Notification;
import com.nemal.repository.NotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void sendInterviewRequestNotification(InterviewRequest request) {
        Notification notification = Notification.builder()
                .recipient(request.getAssignedInterviewer())
                .subject("New Interview Request")
                .message(String.format(
                        "You have a new interview request for candidate %s. " +
                                "Scheduled for %s. Please review and respond.",
                        request.getCandidateName(),
                        request.getPreferredStartDateTime()
                ))
                .type("INTERVIEW_REQUEST")
                .sent(false)
                .build();

        notificationRepository.save(notification);
    }

    public void sendInterviewRequestResponseNotification(InterviewRequest request) {
        String status = request.getStatus().toString();
        Notification notification = Notification.builder()
                .recipient(request.getRequestedBy())
                .subject("Interview Request Response")
                .message(String.format(
                        "Your interview request for candidate %s has been %s by %s.",
                        request.getCandidateName(),
                        status.toLowerCase(),
                        request.getAssignedInterviewer().getFullName()
                ))
                .type("INTERVIEW_REQUEST_RESPONSE")
                .sent(false)
                .build();

        notificationRepository.save(notification);
    }
}