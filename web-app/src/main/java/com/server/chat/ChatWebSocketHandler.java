package com.server.chat;

import com.server.framework.common.DateUtil;
import com.server.framework.common.CommonService;
import com.server.chat.service.ChatUserDetailService;
import com.server.chat.service.ChatUserService;
import com.server.framework.security.SecurityUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ChatWebSocketHandler implements WebSocketHandler {
    
    private static final Map<WebSocketSession, String> activeSessions = new ConcurrentHashMap<>();
    private static final Map<String, String> sessionIdVsFileName = new ConcurrentHashMap<>();
    private static final Map<String, ByteBuffer> sessionIdVsChunkedData = new ConcurrentHashMap<>();
    private static final Logger LOGGER = Logger.getLogger(ChatWebSocketHandler.class.getName());
    
    @Autowired
    private ChatUserService chatUserService;
    
    @Autowired
    private ChatUserDetailService chatUserDetailService;
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String name = getNameFromSession(session);
        String rejoin = getRejoinFromSession(session);
        
        if (name == null || name.trim().isEmpty()) {
            session.close(CloseStatus.BAD_DATA.withReason("invalid_name"));
            return;
        }

        for (String existingName : activeSessions.values()) {
            if (existingName.trim().equalsIgnoreCase(name.trim())) {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("duplicate_name"));
                return;
            }
        }
        
        activeSessions.put(session, name);
        LOGGER.info("New Session connected with id " + session.getId());
        
        try {
            chatUserService.addOrGetUser(name);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating user", e);
            session.close(CloseStatus.SERVER_ERROR.withReason("internal_error"));
            return;
        }
        
        if (Boolean.parseBoolean(rejoin)) {
            session.sendMessage(new TextMessage("rejoined"));
            return;
        }

        session.setBinaryMessageSizeLimit(1024 * 300);
        session.setTextMessageSizeLimit(1024 * 300);
        
        String messageForSender = "<b style='color:green;margin: 70px'>You Joined! [ " + DateUtil.getFormattedCurrentTime() + " ]</b></br></br>";
        String messageForReceiver = "<b style='color:green;margin:70px'>" + name + " Joined!</b></br></br>";

        session.sendMessage(new TextMessage(chatUserDetailService.getPreviousMessage(name)));
        handleMessage(messageForSender, messageForReceiver, name);
    }
    
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String name = activeSessions.get(session);
        if (name == null) {
            return;
        }
        
        if (message instanceof TextMessage) {
            String msg = ((TextMessage) message).getPayload();
            
            if (msg.contains("filename=")) {
                Matcher matcher = Pattern.compile("filename=(.*)&size=(.*)").matcher(msg);
                if (matcher.matches()) {
                    sessionIdVsFileName.put(session.getId(), matcher.group(1));
                    ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.parseInt(matcher.group(2)));
                    sessionIdVsChunkedData.put(session.getId(), byteBuffer);
                }
                return;
            }
            
            if (msg.contains("endoffile123")) {
                writeFile(session);
                return;
            }
            
            if (msg.contains("cancelCurrentFile")) {
                String fileName = sessionIdVsFileName.get(session.getId());
                sessionIdVsFileName.remove(session.getId());
                sessionIdVsChunkedData.remove(session.getId());
                session.sendMessage(new TextMessage("currentFileCancelled"));
                LOGGER.info("File " + fileName + " cancelled successfully");
                return;
            }
            
            handleMessage(getFormattedMessage("You", msg), getFormattedMessage(name, msg), name);
            
        } else if (message instanceof BinaryMessage) {
            handleFile(((BinaryMessage) message).getPayload(), session);
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        LOGGER.log(Level.SEVERE, "Transport error for session " + session.getId(), exception);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        LOGGER.info("Session closed " + session.getId());
        String name = activeSessions.remove(session);
        if (Objects.isNull(name)) {
            return;
        }
        
        String leftOrDisconnected = (closeStatus == CloseStatus.GOING_AWAY || 
                                   closeStatus.getCode() == 1006) ? " Disconnected!" : " Left!";
        
        String messageForSender = "<b style='color:red;margin: 70px'>You" + leftOrDisconnected + 
            " [ " + DateUtil.getFormattedCurrentTime() + " ]</b></br></br>";
        messageForSender = leftOrDisconnected.equals(" Disconnected!") ? 
            messageForSender.replace("You", "") : messageForSender;
        String messageForReceiver = "<b style='color:red;margin: 70px'>" + name + leftOrDisconnected + "</b></br></br>";
        
        handleMessage(messageForSender, messageForReceiver, name);
        chatUserDetailService.addMessage(name, messageForSender);
    }
    
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
    
    private void handleMessage(String messageForSender, String messageForReceiver, String senderName) {
        activeSessions.forEach((session, userName) -> {
            try {
                if (userName.equalsIgnoreCase(senderName)) {
                    chatUserDetailService.addMessage(senderName, messageForSender);
                }
                
                if (session.isOpen()) {
                    if (userName.equalsIgnoreCase(senderName)) {
                        session.sendMessage(new TextMessage(messageForSender));
                    } else {
                        chatUserDetailService.addMessage(userName, messageForReceiver);
                        session.sendMessage(new TextMessage(messageForReceiver));
                    }
                } else {
                    activeSessions.remove(session);
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Exception occurred", e);
            }
        });
    }
    
    private void handleFile(ByteBuffer data, WebSocketSession session) throws IOException {
        String fileName = sessionIdVsFileName.get(session.getId());
        ByteBuffer byteBuffer = sessionIdVsChunkedData.get(session.getId());
        if (byteBuffer == null) {
            return;
        }
        byteBuffer.put(data.array());
        session.sendMessage(new TextMessage("Uploading " + fileName + " [ " + 
            (int) Math.floor(((double) byteBuffer.position() / byteBuffer.capacity()) * 100) + " % completed ]"));
    }
    
    private void writeFile(WebSocketSession session) throws IOException {
        ByteBuffer byteBuffer = sessionIdVsChunkedData.get(session.getId());
        byte[] byteArray = byteBuffer.array();

        new File(SecurityUtil.getUploadsPath()).mkdirs();
        FileOutputStream fileOutputStream = new FileOutputStream(SecurityUtil.getUploadsPath().concat("/") + sessionIdVsFileName.get(session.getId()));
        fileOutputStream.write(byteArray);
        fileOutputStream.close();

        
        String msg = "<a  target='_blank' href='/uploads/" + sessionIdVsFileName.get(session.getId()) + "'>" + sessionIdVsFileName.get(session.getId()) + "</a></br></br>";
        sessionIdVsFileName.remove(session.getId());
        sessionIdVsChunkedData.remove(session.getId());
        
        String name = activeSessions.get(session);
        String messageForSender = getFormattedMessage("You", msg);
        String messageForReceiver = getFormattedMessage(name, msg);
        
        handleMessage(messageForSender, messageForReceiver, name);
        session.sendMessage(new TextMessage("fileuploaddone123"));
    }
    
    private static String getFormattedMessage(String name, String message) {
        return name.equalsIgnoreCase("you") ? 
            "<div class='container1 darker1' style='margin: 10px 250'><b class='user-name'>" + name + "</b> <br><br><p style='margin-left:15px'>" + message + "</p><span class='time-left'>" + DateUtil.getFormattedCurrentTime() + "</span></div><br><br>"
            : "<div class='container darker' style='margin: 10px 0px'><b class='user-name'>" + name + "</b> <br><br><p style='margin-left:15px'>" + message + "</p><span class='time-left'>" + DateUtil.getFormattedCurrentTime() + "</span></div><br><br>";
    }
    
    private String getNameFromSession(WebSocketSession session) {
        return session.getUri() != null ? 
            session.getUri().getQuery() != null ?
                extractParameter(session.getUri().getQuery(), "name") : null : null;
    }
    
    private String getRejoinFromSession(WebSocketSession session) {
        return session.getUri() != null ? 
            session.getUri().getQuery() != null ?
                extractParameter(session.getUri().getQuery(), "rejoin") : "false" : "false";
    }
    
    private String extractParameter(String query, String paramName) {
        if (query == null) return null;
        String[] params = query.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2 && keyValue[0].equals(paramName)) {
                return keyValue[1];
            }
        }
        return null;
    }
}
