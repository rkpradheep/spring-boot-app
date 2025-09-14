package com.server.stream;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.stereotype.Component;

@Component
@ServerEndpoint(value = "/api/v1/live")
public class Subscribers
{
	private static final Logger LOGGER = Logger.getLogger(Subscribers.class.getName());
	public String sessionID;
	public static Map<String, Session> sessionMap = new LinkedHashMap<>();
	public static List<String> newJoineeList = new ArrayList();

	@OnOpen
	public void OnOpen(Session session)
	{
		sessionID = session.getId();
		sessionMap.put(session.getId(), session);
		newJoineeList.add(session.getId());
		StreamServlet.restartBroadCast = true;


		LOGGER.info("Subscriber Session Started " + session.getId());
	}

	@OnClose
	public void onClose(Session ss)
	{
		try
		{
			sessionMap.remove(sessionID);
			ss.close();
			LOGGER.info("Subscriber Session closed " + sessionID);
		}
		catch(IOException e)
		{
		}
	}

}
