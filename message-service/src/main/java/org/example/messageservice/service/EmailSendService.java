package org.example.messageservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.messageservice.entity.MsgCarrier;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Properties;

@Service
public class EmailSendService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void send(MsgCarrier carrier, String to, String subject, String body) {
        try {
            Map<String, Object> config = objectMapper.readValue(carrier.getConfigJson(), Map.class);
            String host = String.valueOf(config.getOrDefault("host", "smtp.qq.com"));
            int port = config.get("port") instanceof Integer ? (int) config.get("port") : 587;
            String username = String.valueOf(config.getOrDefault("username", ""));
            String password = String.valueOf(config.getOrDefault("password", ""));

            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(host); sender.setPort(port);
            sender.setUsername(username); sender.setPassword(password);

            Properties props = sender.getJavaMailProperties();
            props.put("mail.smtp.auth", "true");
            if (port == 465) { props.put("mail.smtp.ssl.enable", "true"); sender.setProtocol("smtps"); }
            else props.put("mail.smtp.starttls.enable", "true");

            MimeMessage mime = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setFrom(username); helper.setTo(to); helper.setSubject(subject); helper.setText(body);
            sender.send(mime);
        } catch (Exception e) {
            throw new RuntimeException("邮件发送失败: " + e.getMessage(), e);
        }
    }
}