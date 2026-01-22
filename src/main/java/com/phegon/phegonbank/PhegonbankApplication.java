package com.phegon.phegonbank;

import com.phegon.phegonbank.auth_users.entity.User;
import com.phegon.phegonbank.enums.NotificationType;
import com.phegon.phegonbank.notification.dtos.NotificationDTO;
import com.phegon.phegonbank.notification.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableAsync
@RequiredArgsConstructor
public class PhegonbankApplication {

    private final NotificationService notificationService;

	public static void main(String[] args) {
		SpringApplication.run(PhegonbankApplication.class, args);
	}

    @Bean
    CommandLineRunner runner(){
        return args -> {
            NotificationDTO notificationDTO = NotificationDTO.builder()
                    .recipient("miaoyiyuan31@gmail.com")
                    .subject("Test email")
                    .body("hi! Testing 123")
                    .type(NotificationType.EMAIL)
                    .build();
            notificationService.sendEmail(notificationDTO, new User());
        };
    }


}
