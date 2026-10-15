package fr.centralesupelec.thuv.mail;

import fr.centralesupelec.thuv.model.Course;
import fr.centralesupelec.thuv.scale_up.model.DeploymentStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender javaMailSender;
    private final MailParameterConfiguration mailParameterConfiguration;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.magic_link.expiration_in_minutes}")
    private long magicLinkExpirationInMinutes;

    public void sendDeploymentErrorMail(DeploymentStatus deploymentStatus) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailParameterConfiguration.getFrom());
            message.setTo(mailParameterConfiguration.getTo());
            message.setSubject(
                    "[Deployment error] Created at: " + deploymentStatus.getCreatedOn().format(dateTimeFormatter)
            );
            message.setText(
                    "Error message:\n"
                    + formatText(
                        deploymentStatus.getBuildErrors()
                    )
                    + "\n\nLogs: \n"
                    + formatText(
                        deploymentStatus.getLogs()
                    )
            );
            javaMailSender.send(message);
        } catch (MailException e) {
            logger.error("Failed to send email: " + e.getMessage());
        }
    }

    public void sendMagicLinkEmail(String recipientEmail, Course course, String token) {
        String magicLink = String.format("%s/loginAccept?token=%s&redirectTo=shell?course_id=%s", frontendUrl, token, course.getId());

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailParameterConfiguration.getFrom());
            message.setTo(recipientEmail);
            message.setSubject("Accès au cours " + course.getTitle());
            message.setText(
                "Bonjour,\n\n"
                +
                "Vous avez demandé un accès au cours " + course.getTitle() + ".\n"
                +
                "Cliquez sur le lien ci-dessous pour vous connecter :\n\n"
                +
                magicLink + "\n\n"
                +
                "Ce lien expirera dans " + Long.toString(magicLinkExpirationInMinutes) + " minutes.\n\n"
                +
                "Cordialement,\nL'équipe support."
            );
            javaMailSender.send(message);
            logger.info("Magic link email sent to {}", recipientEmail);
        } catch (MailException e) {
            logger.error("Failed to send magic link email to {}: {}", recipientEmail, e.getMessage());
        }
    }

    private String formatText(String text) {
        if (text == null) {
            return "";
        }
        String output = "\t";
        output += String.join("\n\t", text.split(System.lineSeparator()));
        return output;
    }
}
