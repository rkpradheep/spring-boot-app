package com.server.stream;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import org.springframework.stereotype.Component;

import com.server.framework.common.CommonService;

@Component
@ServerEndpoint(value = "/api/v1/broadcast")
public class BroadCast
{
	private static final Logger LOGGER = Logger.getLogger(BroadCast.class.getName());

	@OnOpen
	public void OnOpen(Session session)
	{
		LOGGER.info("Broadcast Session Started " + session.getId());
	}

	@OnMessage
	public void broadcast(Session session, ByteBuffer bb, boolean last)
	{
		try
		{
			for(Session sessions : Subscribers.sessionMap.values())
				if(session.isOpen())
					sessions.getBasicRemote().sendBinary(bb, last);
		}
		catch(Exception e)
		{
			LOGGER.log(java.util.logging.Level.SEVERE, "Exception in broadcast", e);
		}
	}

	@OnClose
	public void onClose(Session ss)
	{
		try
		{
			LOGGER.info("Broadcast Session Closed " + ss.getId());
			ss.close();
		}
		catch(IOException e)
		{
		}
	}

	@OnMessage
	public void sendText(Session session, String msg, boolean last)
	{
		try
		{
			if(session.isOpen())
			{
				session.getBasicRemote().sendText("Hello " + msg, last);
			}
		}
		catch(IOException e)
		{
		}
	}

	public static void tel()
	{
		try
		{
			FileInputStream fileInputStream = new FileInputStream(CommonService.HOME_PATH + "/Downloads/ct1-datamigration.mp4");
			ByteBuffer byteBuffer = ByteBuffer.allocate((int) fileInputStream.getChannel().size());
			byte[] b = new byte[1024];
			int i = fileInputStream.getChannel().read(byteBuffer);
			fileInputStream.close();

			for(Session sessions : Subscribers.sessionMap.values())
				if(sessions.isOpen())
					sessions.getBasicRemote().sendBinary(byteBuffer);
		}
		catch(Exception e)
		{
			LOGGER.log(java.util.logging.Level.SEVERE, "Exception in sendData", e);
		}
	}

}
