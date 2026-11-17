package com.drukconnect.drukconnect.mail;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/** Keeps existing plain-text and HTML templates while delivering over HTTPS instead of SMTP. */
public class ResendMailSender extends JavaMailSenderImpl {
    private final RestClient client;

    public ResendMailSender(RestClient client) {
        this.client = client;
        setDefaultEncoding("UTF-8");
    }

    @Override
    protected void doSend(MimeMessage[] messages, Object[] originalMessages) {
        Map<Object, Exception> failures = new LinkedHashMap<>();
        for (int i = 0; i < messages.length; i++) {
            try {
                MimeMessage message = messages[i];
                message.saveChanges();
                Map<String, Object> body = new LinkedHashMap<>();
                List<String> from = addresses(message.getFrom());
                if (from.size() != 1) throw new IllegalArgumentException("Email needs exactly one sender");
                body.put("from", from.getFirst());
                recipients(body, "to", message.getRecipients(Message.RecipientType.TO));
                recipients(body, "cc", message.getRecipients(Message.RecipientType.CC));
                recipients(body, "bcc", message.getRecipients(Message.RecipientType.BCC));
                recipients(body, "reply_to", message.getReplyTo());
                body.put("subject", message.getSubject());
                addContent(message, body);
                Map<?, ?> result = client.post().uri("/emails").body(body).retrieve().body(Map.class);
                if (result == null || !(result.get("id") instanceof String id) || id.isBlank()) {
                    throw new MailSendException("Resend did not return a message ID");
                }
            } catch (Exception error) {
                // Provider response bodies can echo message content; never include them in user-facing errors.
                Exception safe = error instanceof RestClientResponseException response
                        ? new MailSendException("Resend rejected email (HTTP " + response.getStatusCode().value() + "). Check Resend logs and sender configuration.")
                        : new MailSendException("Unable to submit email through Resend. Check email configuration and connectivity.");
                failures.put(originalMessages != null ? originalMessages[i] : messages[i], safe);
            }
        }
        if (!failures.isEmpty()) throw new MailSendException(failures);
    }

    private static List<String> addresses(Address[] values) {
        return values == null ? List.of() : Arrays.stream(values).map(Address::toString).toList();
    }

    private static void recipients(Map<String, Object> body, String name, Address[] values) {
        List<String> list = addresses(values);
        if (!list.isEmpty()) body.put(name, list);
    }

    @SuppressWarnings("unchecked")
    private static void addContent(Part part, Map<String, Object> body) throws Exception {
        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || part.getFileName() != null) {
            List<Map<String, String>> attachments = (List<Map<String, String>>) body.computeIfAbsent("attachments", key -> new ArrayList<>());
            try (var input = part.getInputStream()) {
                attachments.add(Map.of("filename", part.getFileName() == null ? "attachment" : part.getFileName(),
                        "content", Base64.getEncoder().encodeToString(input.readAllBytes())));
            }
        } else if (part.isMimeType("text/plain")) {
            body.merge("text", part.getContent().toString(), (a, b) -> a + "\n" + b);
        } else if (part.isMimeType("text/html")) {
            body.merge("html", part.getContent().toString(), (a, b) -> a + "\n" + b);
        } else if (part.isMimeType("multipart/*")) {
            Multipart content = (Multipart) part.getContent();
            for (int i = 0; i < content.getCount(); i++) addContent(content.getBodyPart(i), body);
        } else {
            throw new IllegalArgumentException("Unsupported email content type");
        }
    }
}
