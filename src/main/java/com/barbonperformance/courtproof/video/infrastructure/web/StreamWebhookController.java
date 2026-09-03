package com.barbonperformance.courtproof.video.infrastructure.web;

import com.barbonperformance.courtproof.video.application.StreamWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks")
public class StreamWebhookController {

    private static final String SIGNATURE_HEADER = "Webhook-Signature";

    private final StreamWebhookService webhooks;

    public StreamWebhookController(StreamWebhookService webhooks) {
        this.webhooks = webhooks;
    }

    // The body is taken as a raw String on purpose: the signature covers the exact bytes
    // Cloudflare sent, and re-serialising a parsed object would not reproduce them.
    @PostMapping("/cloudflare-stream")
    @SecurityRequirements
    @Operation(summary = "Cloudflare Stream status callback")
    ResponseEntity<Void> streamStatus(
            @RequestBody String rawBody,
            @RequestHeader(name = SIGNATURE_HEADER, required = false) String signature) {
        webhooks.handle(rawBody, signature);
        return ResponseEntity.noContent().build();
    }
}
