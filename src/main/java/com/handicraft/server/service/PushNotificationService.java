package com.handicraft.server.service;

import com.handicraft.server.model.PushSubscription;
import com.handicraft.server.repository.PushSubscriptionRepository;
import jakarta.annotation.PostConstruct;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class PushNotificationService {

    @Value("${vapid.public-key}")
    private String vapidPublicKey;

    @Value("${vapid.private-key}")
    private String vapidPrivateKey;

    @Value("${vapid.subject}")
    private String vapidSubject;

    @Autowired
    private PushSubscriptionRepository subscriptionRepo;

    private PushService pushService;

    @PostConstruct
    public void init() throws Exception {
        if (vapidPublicKey.isBlank() || vapidPrivateKey.isBlank()) {
            System.out.println("[Push] VAPID keys not configured — push notifications disabled.");
            return;
        }
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        pushService = new PushService(vapidPublicKey, vapidPrivateKey, vapidSubject);
        System.out.println("[Push] PushService initialised with VAPID keys.");
    }

    public void notifyAdmins(String title, String body, String url) {
        if (pushService == null) return;
        List<PushSubscription> adminSubs = subscriptionRepo.findByRole("admin");
        if (adminSubs.isEmpty()) return;

        String payload = buildPayload(title, body, url);

        CompletableFuture.runAsync(() -> {
            for (PushSubscription sub : adminSubs) {
                try {
                    Notification notification = new Notification(
                            sub.getEndpoint(),
                            sub.getP256dh(),
                            sub.getAuth(),
                            payload.getBytes(StandardCharsets.UTF_8)
                    );
                    HttpResponse response = pushService.send(notification);
                    int status = response.getStatusLine().getStatusCode();

                    // 410 Gone = subscription expired, remove it
                    if (status == 410) {
                        subscriptionRepo.deleteByEndpoint(sub.getEndpoint());
                    }
                } catch (Exception e) {
                    System.err.println("[Push] Failed to send to " + sub.getUserId() + ": " + e.getMessage());
                }
            }
        });
    }

    public List<PushSubscription> listAll() {
        return subscriptionRepo.findAll();
    }

    private String buildPayload(String title, String body, String url) {
        return String.format(
            "{\"title\":\"%s\",\"body\":\"%s\",\"url\":\"%s\"}",
            escape(title), escape(body), url
        );
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
