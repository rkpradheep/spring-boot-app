package com.server.framework.ssl;

import com.server.framework.http.HttpService;
import com.server.framework.http.HttpContext;
import org.json.JSONObject;
import org.json.JSONArray;
import org.springframework.stereotype.Component;

import java.security.*;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.nio.charset.StandardCharsets;

@Component
public class ACMEClient {
    
    private static final Logger LOGGER = Logger.getLogger(ACMEClient.class.getName());

    private static final String ACME_DIRECTORY_URL = "https://acme-v02.api.letsencrypt.org/directory";

    private String directoryUrl;
    private JSONObject directory;
    private KeyPair accountKeyPair;
    private String accountUrl;
    private String nonce;
    
    public ACMEClient() {
        this.directoryUrl = ACME_DIRECTORY_URL;
    }
    
    public boolean initialize() {
        try {
            HttpContext context = new HttpContext(directoryUrl, "GET");
            JSONObject jsonResponse = HttpService.makeNetworkCallStatic(context).getJSONResponse();
            
            if (jsonResponse == null) {
                LOGGER.severe("Failed to fetch ACME directory");
                return false;
            }
            
            this.directory = jsonResponse;

            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4));
            this.accountKeyPair = keyGen.generateKeyPair();

            this.nonce = getNonce();
            
            LOGGER.info("ACME client initialized successfully");
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize ACME client", e);
            return false;
        }
    }
    
    public boolean createAccount(String email) {
        try {
            if (directory == null) {
                throw new IllegalStateException("ACME client not initialized");
            }
            
            String newAccountUrl = directory.getString("newAccount");
            
            JSONObject payload = new JSONObject();
            payload.put("termsOfServiceAgreed", true);
            
            if (email != null && !email.isEmpty()) {
                JSONArray contacts = new JSONArray();
                contacts.put("mailto:" + email);
                payload.put("contact", contacts);
            }
            
            String signedRequest = createSignedRequest(newAccountUrl, payload, null);
            
            HttpContext context = new HttpContext(newAccountUrl, "POST");
            context.setHeader("Content-Type", "application/jose+json");
            context.setBody(new JSONObject(signedRequest));
            
            JSONObject jsonResponse = HttpService.makeNetworkCallStatic(context).getJSONResponse();
            
            if (jsonResponse != null) {
                this.accountUrl = newAccountUrl.replace("newAccount", "account");
                LOGGER.info("ACME account created successfully");
                return true;
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create ACME account", e);
        }
        
        return false;
    }
    
    public Map<String, Object> initiateChallenge(String domain, String challengeType) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (directory == null || accountUrl == null) {
                throw new IllegalStateException("ACME client not initialized or account not created");
            }

            String newOrderUrl = directory.getString("newOrder");
            
            JSONObject orderPayload = new JSONObject();
            JSONArray identifiers = new JSONArray();
            JSONObject identifier = new JSONObject();
            identifier.put("type", "dns");
            identifier.put("value", domain);
            identifiers.put(identifier);
            orderPayload.put("identifiers", identifiers);
            
            String signedOrderRequest = createSignedRequest(newOrderUrl, orderPayload, accountUrl);
            
            HttpContext orderContext = new HttpContext(newOrderUrl, "POST");
            orderContext.setHeader("Content-Type", "application/jose+json");
            orderContext.setBody(new JSONObject(signedOrderRequest));
            
            JSONObject order = HttpService.makeNetworkCallStatic(orderContext).getJSONResponse();
            
            if (order != null) {
                JSONArray authorizations = order.getJSONArray("authorizations");
                
                if (!authorizations.isEmpty()) {
                    String authzUrl = authorizations.getString(0);

                    HttpContext authzContext = new HttpContext(authzUrl, "GET");
                    JSONObject authz = HttpService.makeNetworkCallStatic(authzContext).getJSONResponse();
                    
                    if (authz != null) {
                        JSONArray challenges = authz.getJSONArray("challenges");

                        for (int i = 0; i < challenges.length(); i++) {
                            JSONObject challenge = challenges.getJSONObject(i);
                            String type = challenge.getString("type");
                            
                            if (challengeType.equals(type)) {
                                String token = challenge.getString("token");
                                String challengeUrl = challenge.getString("url");
                                
                                result.put("success", true);
                                result.put("challenge_type", type);
                                result.put("token", token);
                                result.put("challenge_url", challengeUrl);
                                result.put("domain", domain);
                                
                                if ("dns-01".equals(type)) {
                                    String keyAuthorization = token + "." + getJWKThumbprint();
                                    String dnsValue = base64UrlEncode(sha256(keyAuthorization.getBytes(StandardCharsets.UTF_8)));
                                    
                                    result.put("dns_name", "_acme-challenge." + domain);
                                    result.put("dns_value", dnsValue);
                                    result.put("instructions", "Add a TXT record with name '_acme-challenge." + domain + "' and value '" + dnsValue + "'");
                                    
                                } else if ("http-01".equals(type)) {
                                    String keyAuthorization = token + "." + getJWKThumbprint();
                                    
                                    result.put("http_path", "/.well-known/acme-challenge/" + token);
                                    result.put("http_content", keyAuthorization);
                                    result.put("instructions", "Make sure your server returns '" + keyAuthorization + "' when accessing http://" + domain + "/.well-known/acme-challenge/" + token);
                                }
                                
                                return result;
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initiate challenge", e);
        }
        
        result.put("success", false);
        result.put("error", "Failed to initiate challenge");
        return result;
    }
    
    public Map<String, Object> verifyAndGetCertificate(String challengeUrl, String domain, byte[] csrBytes) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            JSONObject challengePayload = new JSONObject();
            String signedChallengeRequest = createSignedRequest(challengeUrl, challengePayload, accountUrl);
            
            HttpContext challengeContext = new HttpContext(challengeUrl, "POST");
            challengeContext.setHeader("Content-Type", "application/jose+json");
            challengeContext.setBody(new JSONObject(signedChallengeRequest));
            
            JSONObject challengeResponse = HttpService.makeNetworkCallStatic(challengeContext).getJSONResponse();
            
            if (challengeResponse != null) {
                Thread.sleep(5000);

                String finalizeUrl = directory.getString("newOrder").replace("newOrder", "finalize");
                
                JSONObject finalizePayload = new JSONObject();
                finalizePayload.put("csr", base64UrlEncode(csrBytes));
                
                String signedFinalizeRequest = createSignedRequest(finalizeUrl, finalizePayload, accountUrl);
                
                HttpContext finalizeContext = new HttpContext(finalizeUrl, "POST");
                finalizeContext.setHeader("Content-Type", "application/jose+json");
                finalizeContext.setBody(new JSONObject(signedFinalizeRequest));
                
                JSONObject finalizeResult = HttpService.makeNetworkCallStatic(finalizeContext).getJSONResponse();
                
                if (finalizeResult != null) {
                    
                    if (finalizeResult.has("certificate")) {
                        String certificateUrl = finalizeResult.getString("certificate");

                        HttpContext certContext = new HttpContext(certificateUrl, "GET");
                        String certificate = HttpService.makeNetworkCallStatic(certContext).getStringResponse();
                        
                        if (certificate != null && certificate.contains("BEGIN CERTIFICATE")) {
                            result.put("success", true);
                            result.put("certificate", certificate);
                            result.put("domain", domain);
                            result.put("message", "Certificate generated successfully");
                            return result;
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to verify challenge and get certificate", e);
        }
        
        result.put("success", false);
        result.put("error", "Failed to verify challenge and get certificate");
        return result;
    }
    
    private String getNonce() {
        try {
            String newNonceUrl = directory.getString("newNonce");
            HttpContext context = new HttpContext(newNonceUrl, "HEAD");
            HttpService.makeNetworkCallStatic(context);
            return "dummy-nonce-" + System.currentTimeMillis();
        } catch (Exception e) {
            return "dummy-nonce-" + System.currentTimeMillis();
        }
    }
    
    private String createSignedRequest(String url, JSONObject payload, String kid) throws Exception {
        JSONObject header = new JSONObject();
        header.put("alg", "RS256");
        header.put("nonce", nonce);
        header.put("url", url);
        
        if (kid != null) {
            header.put("kid", kid);
        } else {
            header.put("jwk", getJWK());
        }
        
        String encodedHeader = base64UrlEncode(header.toString().getBytes(StandardCharsets.UTF_8));
        String encodedPayload = base64UrlEncode(payload.toString().getBytes(StandardCharsets.UTF_8));
        
        String signingInput = encodedHeader + "." + encodedPayload;
        String signature = sign(signingInput);
        
        JSONObject jws = new JSONObject();
        jws.put("protected", encodedHeader);
        jws.put("payload", encodedPayload);
        jws.put("signature", signature);
        
        return jws.toString();
    }
    
    private JSONObject getJWK() throws Exception {
        JSONObject jwk = new JSONObject();
        jwk.put("kty", "RSA");
        jwk.put("use", "sig");
        jwk.put("alg", "RS256");
        return jwk;
    }
    
    private String getJWKThumbprint() throws Exception {
        JSONObject jwk = getJWK();
        String jwkJson = jwk.toString();
        byte[] hash = sha256(jwkJson.getBytes(StandardCharsets.UTF_8));
        return base64UrlEncode(hash);
    }
    
    private String sign(String data) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(accountKeyPair.getPrivate());
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        byte[] signatureBytes = signature.sign();
        return base64UrlEncode(signatureBytes);
    }
    
    private byte[] sha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(data);
    }
    
    private String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
}
