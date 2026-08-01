package com.securebank.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "securebank.notification")
public record NotificationProperties(Sms sms, Email email, InApp inApp) {
  public NotificationProperties {
    sms = sms == null ? new Sms("log", "SecureBank", new Twilio("", "", "")) : sms;
    email = email == null ? new Email("log", "no-reply@securebank.lk") : email;
    inApp = inApp == null ? new InApp(true) : inApp;
  }

  public record Sms(String provider, String sender, Twilio twilio) {
    public Sms {
      provider = provider == null || provider.isBlank() ? "log" : provider;
      sender = sender == null || sender.isBlank() ? "SecureBank" : sender;
      twilio = twilio == null ? new Twilio("", "", "") : twilio;
    }
  }

  public record Twilio(String accountSid, String authToken, String fromNumber) {}

  public record Email(String provider, String fromAddress) {
    public Email {
      provider = provider == null || provider.isBlank() ? "log" : provider;
      fromAddress =
        fromAddress == null || fromAddress.isBlank() ? "no-reply@securebank.lk" : fromAddress;
    }
  }

  public record InApp(boolean enabled) {}
}
