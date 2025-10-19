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

    public static String postMessageToChannel(String message) {
        try {
            URL url = new URL("https://cliq.zoho.in/company/60047444754/api/v2/channelsbyname/ext:pradheep/message?zapikey=" + AppProperties.getProperty("cliq.zapi.key"));

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
