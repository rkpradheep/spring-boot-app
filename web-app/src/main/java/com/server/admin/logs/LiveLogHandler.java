package com.server.admin.logs;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Component;

@Component
@ServerEndpoint(value = "/api/v1/admin/live/logs")
public class LiveLogHandler
{
	private static final Logger LOGGER = Logger.getLogger(LiveLogHandler.class.getName());

	private static final List<Session> liveSessionList = new ArrayList<>();

	@OnOpen
	public void OnOpen(Session session)
	{
		liveSessionList.add(session);
		LogWatchService.start();
		LOGGER.info("Session joined : " + session.getId());
	}

	public static void broadcast(String message)
	{
		try
		{
			for(Session session : liveSessionList)
			{
				if(session.isOpen())
				{
					session.getBasicRemote().sendText(message);
				}
				else
				{
					liveSessionList.remove(session);
				}
			}
		}
		catch(Exception e)
		{
			LOGGER.log(Level.SEVERE, "Exception occurred", e);
		}
	}

	@OnClose
	public void onClose(Session session)
	{
		handleSessionClose(session);
	}

	@OnError
	public void onError(Session session, Throwable throwable)
	{
		handleSessionClose(session);
		LOGGER.log(Level.SEVERE, "Error occurred for session " + session.getId(), throwable);
	}

	private static void handleSessionClose(Session session)
	{
		try
		{
			liveSessionList.remove(session);
			LOGGER.info("Session Closed " + session.getId());
			if(liveSessionList.isEmpty())
			{
				LogWatchService.stop();
			}
			session.close();
		}
		catch(IOException e)
		{
			LOGGER.log(Level.SEVERE, "Exception occurred", e);
		}
	}

}
