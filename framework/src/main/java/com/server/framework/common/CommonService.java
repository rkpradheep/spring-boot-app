package com.server.framework.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.server.framework.http.HttpContext;
import com.server.framework.http.HttpService;

public class CommonService {
    public static final String HOME_PATH = System.getenv("APP_SERVER_HOME");

    private static final Logger LOGGER = Logger.getLogger(CommonService.class.getName());
    private static final Gson GSON = new Gson();

    public static String encryptData(PublicKey publicKey, String plainText) {
        try {
            byte[] plainBytes = plainText.getBytes(StandardCharsets.UTF_8);
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return Base64.encodeBase64String(cipher.doFinal(plainBytes));
        } catch (Exception e) {
            return "";
        }
    }

    public static String decryptData(PrivateKey privateKey, String cipherText) throws Exception {
        byte[] plainBytes = Base64.decodeBase64(cipherText.getBytes("UTF-8"));
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return new String(cipher.doFinal(plainBytes));
    }

    public static String postMessageToBot(String message) {
        try {
            URL url = new URL("https://cliq.zoho.in/api/v2/bots/myserver/message?zapikey=" + AppProperties.getProperty("cliq.zapi.key"));

            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("POST");

            httpURLConnection.setDoOutput(true);
            httpURLConnection.setRequestProperty("Content-Type", "application/json");

            JSONObject payload = new JSONObject();
            payload.put("text", message);
            httpURLConnection.getOutputStream().write(payload.toString().getBytes(StandardCharsets.UTF_8));

            return getResponse(httpURLConnection.getResponseCode() != 200 ? httpURLConnection.getErrorStream() : httpURLConnection.getInputStream());
        } catch (Exception e) {
            return "";
        }
    }

    public static String getDefaultChannelUrl()
    {
        return AppProperties.getProperty("cliq.channel.url") + "?zapikey=" + AppProperties.getProperty("cliq.zapi.key");
    }

    public static String postMessageToChannel(String message) {
        try {

            String url = "https://cliq.zoho.in/company/60047444754/api/v2/channelsbyname/ext:pradheep/message?zapikey=" + AppProperties.getProperty("cliq.zapi.key");
            return postMessageToChannel(url, message);
        } catch (Exception e) {
            return "";
        }
    }

    public static String postMessageToChannel(String urlString, String message) {
        try {
            HttpContext httpContext = new HttpContext(urlString, "POST");

            JSONObject payload = new JSONObject();
            payload.put("text", message);
            payload.put("sync_message", true);
            payload.put("bot", new JSONObject().put("name", "Build Automation Bot").put("image", "data:image/png;base64,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"));
            httpContext.setBody(payload);
            String response = AppContextHolder.getBean(HttpService.class).makeNetworkCall(httpContext).getStringResponse();
            return new JSONObject(response).getString("message_id");
        } catch (Exception e) {
            return "";
        }
    }

    public static String postMessageToChat(String chatID, String message, String replyTo, String accessToken) {
        try {
            URL url = new URL("https://cliq.zoho.com/company/64396901/api/v2/chats/{CHAT_ID}/message".replace("{CHAT_ID}", chatID));

            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("POST");

            httpURLConnection.setDoOutput(true);
            httpURLConnection.setRequestProperty("Content-Type", "application/json");
            httpURLConnection.setRequestProperty("Authorization", "Bearer " + accessToken);

            JSONObject payload = new JSONObject();
            payload.put("text", message);
            payload.put("reply_to", replyTo);

            JSONObject bot = new JSONObject();
            bot.put("name", "Reminder Bot");
            bot.put("image", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRB2vYCNle16ALEpxdACttqEVBnI-2lX9fVh5o3kCxYv3rZ9xkk6DDDNKN-9VVTcdXq7Mc&usqp=CAU");

            payload.put("bot", bot);
            payload.put("sync_message", true);

            httpURLConnection.getOutputStream().write(payload.toString().getBytes(StandardCharsets.UTF_8));

            return getResponse(httpURLConnection.getResponseCode() != 200 ? httpURLConnection.getErrorStream() : httpURLConnection.getInputStream());
        } catch (Exception e) {
            return "";
        }
    }

    public static String getResponse(InputStream inputStream) throws Exception {
        StringBuilder output = new StringBuilder();

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                output.append(line);
                output.append("\n");
            }
        }

        return output.toString();
    }

    public static JSONObject getZohoSecrets(String dc) {
        dc = Arrays.asList("local", "dev").contains(dc) ? dc : "us";

        JSONObject oauthCredentials = new JSONObject();

        oauthCredentials.put("client_id", AppProperties.getProperty("oauth.$.client.id".replace("$", dc)));
        oauthCredentials.put("client_secret", AppProperties.getProperty("oauth.$.client.secret".replace("$", dc)));
        oauthCredentials.put("redirect_uri", AppProperties.getProperty("oauth.$.client.redirecturi".replace("$", dc)));

        return oauthCredentials;
    }

    public static String readFileAsString(File file) throws IOException {
        FileReader fileReader = new FileReader(file);
        StringWriter stringWriter = new StringWriter();

        IOUtils.copy(fileReader, stringWriter);

        return stringWriter.toString();
    }

    public static Object getJSONFromString(String value) {
        try {
            return new JSONObject(value);
        } catch (Exception e) {
            try {
                return new JSONArray(value);
            } catch (Exception e1) {
                return null;
            }
        }
    }

    public static JSONObject covertPOJOToJSON(Object pojoObject, Class<?> type) {
        return new JSONObject(GSON.toJson(pojoObject, type));
    }

    public static String getAESEncryptedValue(String payLoad) throws Exception {
        String key = AppProperties.getProperty("server.aes.key");
        String iv = AppProperties.getProperty("server.aes.iv");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(Base64.decodeBase64(key), "AES");

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(Base64.decodeBase64(iv)));
        byte[] encryptedBytes = cipher.doFinal(payLoad.getBytes());

        return org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(encryptedBytes);
    }

    public static String getAESDecryptedValue(String encryptedData) throws Exception {
        String key = AppProperties.getProperty("server.aes.key");
        String iv = AppProperties.getProperty("server.aes.iv");

        byte[] data = org.apache.commons.codec.binary.Base64.decodeBase64(encryptedData);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec secretKey = new SecretKeySpec(Base64.decodeBase64(key), "AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(Base64.decodeBase64(iv)));
        byte[] actualData = cipher.doFinal(data);

        return new String(actualData);
    }

    public static Pair<String, String> generateAESEncryptionKey() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[32];
        secureRandom.nextBytes(key);
        return new ImmutablePair<>(Base64.encodeBase64String(key), Base64.encodeBase64String(RandomStringUtils.randomAlphanumeric(16).getBytes()));
    }
}
