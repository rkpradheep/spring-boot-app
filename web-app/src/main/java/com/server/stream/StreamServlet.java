package com.server.stream;

import jakarta.servlet.http.HttpServlet;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import org.springframework.stereotype.Component;

@Component
@ServerEndpoint(value = "/api/v1/stream")
public class StreamServlet extends HttpServlet
{
	private static final Logger LOGGER = Logger.getLogger(StreamServlet.class.getName());
	public static boolean broadCastStarted = false;
	public static boolean restartBroadCast = false;
	public static boolean restartSignalSent = false;

	@OnOpen
	public void OnOpen(Session session) throws IOException
	{
		if(broadCastStarted)
		{
			session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "Broadcasting already"));
			return;
		}
		session.setMaxBinaryMessageBufferSize(1024 * 999);
		broadCastStarted = true;
	}
	@OnMessage
	public void broadcast(ByteBuffer bb, Session session)
	{
		try
		{
			if(restartBroadCast)
			{
				if(restartSignalSent)
				{
					return;
				}
				for(Session sessionJoined : Subscribers.sessionMap.values())
				{
					if(Subscribers.newJoineeList.contains(sessionJoined.getId()))
					{
						Subscribers.newJoineeList.remove(sessionJoined.getId());
						continue;
					}
					if(session.isOpen())
						sessionJoined.getBasicRemote().sendText("reset");
				}
				session.getBasicRemote().sendText("reset");
				restartSignalSent = true;
				return;
			}

			for(Session sessions : Subscribers.sessionMap.values())
				if(session.isOpen())
					sessions.getBasicRemote().sendBinary(bb);
		}
		catch(Throwable e)
		{
			LOGGER.log(java.util.logging.Level.SEVERE, "Exception in binaryMessage", e);
		}
	}

	@OnMessage
	public void messageFromBroadCaster(Session session, String message) throws IOException
	{
		LOGGER.info("Restarted");
		restartBroadCast = false;
		restartSignalSent = false;

		session.getBasicRemote().sendText("restartACK");
	}

	@OnError
	public void onError(Session session, Throwable throwable) {
		LOGGER.severe("com.server.chat.WebSocket Error: " + throwable.getMessage());
		LOGGER.log(java.util.logging.Level.SEVERE, "WebSocket Error", throwable);
	}

	@OnClose
	public void onClose(Session session, CloseReason closeReason)
	{
		restartBroadCast = false;
		restartSignalSent = false;
		broadCastStarted = false;
		LOGGER.info("Closed " + closeReason.getCloseCode());
	}
}
