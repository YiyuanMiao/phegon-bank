package com.phegon.phegonbank.notification.dtos;

import com.phegon.phegonbank.auth_users.entity.User;
import com.phegon.phegonbank.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;


@Data
@Builder
@Table(name="users")
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDTO {
    private Long id;

    private String subject;
    private String recipient;
    private String body;

    private NotificationType type; // email, sms, push

    private LocalDateTime createdAt;

    private String templateName;
    private Map<String, Object> templateVariables;


}
