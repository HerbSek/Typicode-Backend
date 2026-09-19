package herbert.task.app.typicode.backend.utils;

import jakarta.ejb.Stateless;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.activation.DataHandler;
import jakarta.ejb.Asynchronous;
import jakarta.inject.Inject;
//import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.util.Map;
import java.util.Properties;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

@Stateless
@Asynchronous
public class EmailUtil{

   
    private String smtpHost;
    private String smtpPort;    
    private String smtpUser;
    private String smtpPassword;

    public EmailUtil(){
     Config config = ConfigProvider.getConfig();
     this.smtpHost = config.getValue("mail.smtp.host", String.class);
     this.smtpPassword = config.getValue("mail.smtp.password", String.class);
     this.smtpPort = config.getValue("mail.smtp.port", String.class);
     this.smtpUser = config.getValue("mail.smtp.user", String.class);
    }

    public void sendSmtpEmail(String recipient, String subject, String body) throws MessagingException {

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.ssl.trust", smtpHost);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUser, smtpPassword);
            }
        });
        try {
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(smtpUser, "TYPICODE"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
        message.setSubject(subject);
        message.setContent(body, "text/html; charset=UTF-8");
        Transport.send(message);
        }
        catch(Exception e){
         e.printStackTrace();
         System.out.println("Email not sent....");
         System.out.println(e);
        }
    }

//    public void sendBrandedEmail(String recipient, String subject, String body, Map<String, byte[]> inlineImages) throws MessagingException {
//        sendBrandedEmail(recipient, subject, body, inlineImages, null);
//    }
//
//    public void sendBrandedEmail(String recipient, String subject, String body,
//                                 Map<String, byte[]> inlineImages,
//                                 Map<String, byte[]> attachments) throws MessagingException {
//        Properties props = new Properties();
//        props.put("mail.smtp.auth", "true");
//        props.put("mail.smtp.ssl.enable", "true");
//        props.put("mail.smtp.host", smtpHost);
//        props.put("mail.smtp.port", smtpPort);
//        props.put("mail.smtp.ssl.trust", smtpHost);
//
//        Session session = Session.getInstance(props, new Authenticator() {
//            @Override
//            protected PasswordAuthentication getPasswordAuthentication() {
//                return new PasswordAuthentication(smtpUser, smtpPassword);
//            }
//        });
//
//        try {
//            Message message = new MimeMessage(session);
//            message.setFrom(new InternetAddress(smtpUser, "AGORA"));
//            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
//            message.setSubject(subject);
//
//            // HTML + its cid: images; the whole message for every sender except bulk ticket orders.
//            MimeMultipart related = new MimeMultipart("related");
//
//            MimeBodyPart messageBodyPart = new MimeBodyPart();
//            messageBodyPart.setContent(body, "text/html; charset=UTF-8");
//            related.addBodyPart(messageBodyPart);
//
//            if (inlineImages != null) {
//                for (Map.Entry<String, byte[]> entry : inlineImages.entrySet()) {
//                    MimeBodyPart imagePart = new MimeBodyPart();
//                    ByteArrayDataSource ds = new ByteArrayDataSource(entry.getValue(), "image/png");
//                    imagePart.setDataHandler(new DataHandler(ds));
//                    imagePart.setHeader("Content-ID", "<" + entry.getKey() + ">");
//                    imagePart.setDisposition(MimeBodyPart.INLINE);
//                    related.addBodyPart(imagePart);
//                }
//            }
//
//            if (attachments == null || attachments.isEmpty()) {
//                // Unchanged shape: a flat multipart/related, exactly as before.
//                message.setContent(related);
//            } else {
//                // Attachments go in a multipart/mixed wrapper, not the related part — clients render
//                // files there inconsistently, often hiding them or duplicating the body.
//                MimeMultipart mixed = new MimeMultipart("mixed");
//
//                MimeBodyPart relatedWrapper = new MimeBodyPart();
//                relatedWrapper.setContent(related);
//                mixed.addBodyPart(relatedWrapper);
//
//                for (Map.Entry<String, byte[]> entry : attachments.entrySet()) {
//                    String filename = entry.getKey();
//                    MimeBodyPart filePart = new MimeBodyPart();
//                    ByteArrayDataSource ds = new ByteArrayDataSource(entry.getValue(), contentTypeFor(filename));
//                    filePart.setDataHandler(new DataHandler(ds));
//                    filePart.setFileName(filename);
//                    filePart.setDisposition(MimeBodyPart.ATTACHMENT);
//                    mixed.addBodyPart(filePart);
//                }
//
//                message.setContent(mixed);
//            }
//            Transport.send(message);
//            System.out.println("Branded email with attachments sent successfully to " + recipient);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.out.println("Failed to send branded email: " + e.getMessage());
//        }
//    }
//
//    /** Content type from the filename; a PDF sent as octet-stream becomes an unnamed download instead of opening in the phone's viewer. */
//    private static String contentTypeFor(String filename) {
//        if (filename == null) return "application/octet-stream";
//        String lower = filename.toLowerCase();
//        if (lower.endsWith(".pdf")) return "application/pdf";
//        if (lower.endsWith(".png")) return "image/png";
//        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
//        if (lower.endsWith(".csv")) return "text/csv";
//        return "application/octet-stream";
//    }
}
