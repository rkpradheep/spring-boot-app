package com.server.framework.common;

import com.server.framework.ssl.ACMEClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/v1/ssl")
public class SSLCertificateController {
    
    private static final Logger LOGGER = Logger.getLogger(SSLCertificateController.class.getName());
    
    @Autowired
    private ACMEClient acmeClient;

    private final Map<String, Map<String, Object>> challengeStore = new ConcurrentHashMap<>();

    @PostMapping("/sign/initiate")
    public ResponseEntity<Map<String, Object>> initiateSigning(
            @RequestParam("domain") String domain,
            @RequestParam("challengeType") String challengeType,
            @RequestParam(value = "email", required = false) String email) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (!acmeClient.initialize()) {
                response.put("success", false);
                response.put("message", "Failed to initialize ACME client");
                return ResponseEntity.badRequest().body(response);
            }

            if (email != null && !email.isEmpty()) {
                acmeClient.createAccount(email);
            }

            Map<String, Object> challengeResult = acmeClient.initiateChallenge(domain, challengeType);
            
            if ((Boolean) challengeResult.get("success")) {
                String challengeId = "challenge_" + System.currentTimeMillis();
                challengeStore.put(challengeId, challengeResult);
                
                response.put("success", true);
                response.put("message", "Challenge initiated successfully");
                response.put("challengeId", challengeId);
                response.put("challengeType", challengeResult.get("challenge_type"));
                response.put("domain", challengeResult.get("domain"));
                
                if ("dns-01".equals(challengeType)) {
                    response.put("dnsName", challengeResult.get("dns_name"));
                    response.put("dnsValue", challengeResult.get("dns_value"));
                    response.put("instructions", challengeResult.get("instructions"));
                } else if ("http-01".equals(challengeType)) {
                    response.put("httpPath", challengeResult.get("http_path"));
                    response.put("httpContent", challengeResult.get("http_content"));
                    response.put("instructions", challengeResult.get("instructions"));
                }
                
            } else {
                response.put("success", false);
                response.put("message", "Failed to initiate challenge");
                response.put("error", challengeResult.get("error"));
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initiating SSL certificate signing", e);
            response.put("success", false);
            response.put("message", "Error initiating certificate signing: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sign/verify")
    public ResponseEntity<Map<String, Object>> verifySigning(
            @RequestParam("challengeId") String challengeId,
            @RequestParam("csr") MultipartFile csrFile) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> challengeData = challengeStore.get(challengeId);
            
            if (challengeData == null) {
                response.put("success", false);
                response.put("message", "Invalid challenge ID");
                return ResponseEntity.badRequest().body(response);
            }
            
            String challengeUrl = (String) challengeData.get("challenge_url");
            String domain = (String) challengeData.get("domain");

            byte[] csrBytes = csrFile.getBytes();

            Map<String, Object> certResult = acmeClient.verifyAndGetCertificate(challengeUrl, domain, csrBytes);
            
            if ((Boolean) certResult.get("success")) {
                response.put("success", true);
                response.put("message", "Certificate generated successfully");
                response.put("certificate", certResult.get("certificate"));
                response.put("domain", certResult.get("domain"));

                challengeStore.remove(challengeId);
                
            } else {
                response.put("success", false);
                response.put("message", "Failed to verify challenge and generate certificate");
                response.put("error", certResult.get("error"));
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error verifying SSL certificate signing", e);
            response.put("success", false);
            response.put("message", "Error verifying certificate signing: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/challenge/status/{challengeId}")
    public ResponseEntity<Map<String, Object>> getChallengeStatus(@PathVariable String challengeId) {
        Map<String, Object> response = new HashMap<>();
        
        Map<String, Object> challengeData = challengeStore.get(challengeId);
        
        if (challengeData != null) {
            response.put("success", true);
            response.put("challengeData", challengeData);
        } else {
            response.put("success", false);
            response.put("message", "Challenge not found");
        }
        
        return ResponseEntity.ok(response);
    }
}
