package com.ansh.musiclibrary.notificationservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmailWorker {
  private final NoticeRepository repo;
  private final JavaMailSender sender;
  private final String from;

  public EmailWorker(NoticeRepository r, JavaMailSender s, @Value("${app.email-from}") String f) {
    repo = r;
    sender = s;
    from = f;
  }

  @Scheduled(fixedDelay = 30000)
  public void send() {
    for (var n : repo.findTop20ByEmailStatusAndAttemptsLessThan("PENDING", 5)) {
      try {
        var m = new SimpleMailMessage();
        m.setFrom(from);
        m.setTo(n.email);
        m.setSubject("New music in your library");
        m.setText(n.message + ". Open your Music Library to listen.");
        sender.send(m);
        n.emailStatus = "SENT";
      } catch (Exception e) {
        n.attempts++;
        if (n.attempts >= 5) n.emailStatus = "FAILED";
      }
      repo.save(n);
    }
  }
}
