package org.example.messageservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.messageservice.config.DevopsMailProperties;
import org.example.messageservice.entity.MsgCarrier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Service
public class EmailSendService {

    private final JavaMailSender defaultMailSender;
    private final DevopsMailProperties mailProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public EmailSendService(JavaMailSender defaultMailSender, DevopsMailProperties mailProperties) {
        this.defaultMailSender = defaultMailSender;
        this.mailProperties = mailProperties;
    }

    public void send(MsgCarrier carrier, String to, String subject, String body) {
        JavaMailSender sender = carrier != null ? buildFromCarrier(carrier) : defaultMailSender;
        String from = resolveFrom(carrier);
        MimeMessage message = sender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            sender.send(message);
        } catch (Exception e) {
            throw new IllegalStateException("邮件发送失败: " + e.getMessage(), e);
        }
    }

    private String resolveFrom(MsgCarrier carrier) {
        if (carrier != null && carrier.getConfigJson() != null && !carrier.getConfigJson().isBlank()) {
            try {
                JsonNode node = objectMapper.readTree(carrier.getConfigJson());
                if (node.hasNonNull("from")) {
                    return node.get("from").asText();
                }
                if (node.hasNonNull("username")) {
                    return node.get("username").asText();
                }
            } catch (Exception ignored) {
                // fall through
            }
        }
        if (mailProperties.getFrom() != null && !mailProperties.getFrom().isBlank()) {
            return mailProperties.getFrom();
        }
        if (defaultMailSender instanceof JavaMailSenderImpl impl && impl.getUsername() != null) {
            return impl.getUsername();
        }
        throw new IllegalStateException("未配置发件人 devops.mail.from");
    }

    private JavaMailSender buildFromCarrier(MsgCarrier carrier) {
        try {
            JsonNode node = objectMapper.readTree(carrier.getConfigJson());
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(text(node, "host", "smtp.qq.com"));
            sender.setPort(node.has("port") ? node.get("port").asInt(465) : 465);
            sender.setUsername(text(node, "username", null));
            sender.setPassword(text(node, "password", null));
            sender.setDefaultEncoding(StandardCharsets.UTF_8.name());

            Properties props = sender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", String.valueOf(sender.getPort()));
            return sender;
        } catch (Exception e) {
            throw new IllegalStateException("载体 SMTP 配置无效", e);
        }
    }

    private String text(JsonNode node, String field, String defaultValue) {
        return node.hasNonNull(field) ? node.get(field).asText() : defaultValue;
    }
}
