package com.barbonperformance.analysis.video.infrastructure.web;

import com.barbonperformance.analysis.video.application.StreamWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    // The local provider (tusd) cannot attach a static header to its post-finish hook, so the
    // shared secret rides in the hook URL's query string. Only reachable when
    // analysis.video.provider=local; the active StreamWebhooks bean rejects anything else.
    @PostMapping("/tusd")
    @SecurityRequirements
    @Operation(summary = "tusd post-finish callback (local video provider)")
    ResponseEntity<String> tusdStatus(
            @RequestBody String rawBody,
            @RequestParam(name = "token", required = false) String token) {
        webhooks.handle(rawBody, token);
        // tusd parses the hook response as JSON and logs an error on an empty body, so return
        // an empty object — there is nothing for tusd to act on.
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body("{}");
    }
}
