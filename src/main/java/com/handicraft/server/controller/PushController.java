package com.handicraft.server.controller;

import com.handicraft.server.model.PushSubscription;
import com.handicraft.server.repository.PushSubscriptionRepository;
import com.handicraft.server.service.PushNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/push")
public class PushController {

    @Autowired
    private PushSubscriptionRepository subscriptionRepo;

    @Autowired
    private PushNotificationService pushNotificationService;

    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(@RequestBody Map<String, String> body) {
        String endpoint = body.get("endpoint");
        if (endpoint == null || endpoint.isBlank()) {
            return ResponseEntity.badRequest().body("endpoint is required");
        }

        Optional<PushSubscription> existing = subscriptionRepo.findByEndpoint(endpoint);
        PushSubscription sub = existing.orElse(new PushSubscription());

        sub.setEndpoint(endpoint);
        sub.setP256dh(body.get("p256dh"));
        sub.setAuth(body.get("auth"));
        sub.setUserId(body.getOrDefault("userId", "unknown"));
        sub.setRole(body.getOrDefault("role", "store_manager"));
        sub.setCreatedAt(System.currentTimeMillis());

        PushSubscription saved = subscriptionRepo.save(sub);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @DeleteMapping("/unsubscribe")
    public ResponseEntity<?> unsubscribe(@RequestBody Map<String, String> body) {
        String endpoint = body.get("endpoint");
        if (endpoint != null) {
            subscriptionRepo.deleteByEndpoint(endpoint);
        }
        return ResponseEntity.ok("Unsubscribed");
    }

    /** Debug: list all stored subscriptions (admin use only). */
    @GetMapping("/subscriptions")
    public ResponseEntity<List<PushSubscription>> listSubscriptions() {
        return ResponseEntity.ok(subscriptionRepo.findAll());
    }

    /** Fire a real push to all admin subscriptions, for manual testing. */
    @PostMapping("/test")
    public ResponseEntity<?> sendTestNotification() {
        List<PushSubscription> adminSubs = subscriptionRepo.findByRole("admin");
        if (adminSubs.isEmpty()) {
            return ResponseEntity.ok("No admin subscriptions stored — subscribe first.");
        }
        pushNotificationService.notifyAdmins(
                "Test Push",
                "Push notifications are working correctly!",
                "/"
        );
        return ResponseEntity.ok("Test push triggered for " + adminSubs.size() + " admin subscription(s).");
    }
}
