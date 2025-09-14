package com.server.snakegame;

import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
@ServerEndpoint(value = "/snakegame")
public class SnakeGameWebSocketHandler
{

	private static final Logger LOGGER = Logger.getLogger(SnakeGameWebSocketHandler.class.getName());

	@OnOpen
	public void onOpen(Session session) throws IOException
	{
		List<String> gameCodeParam = session.getRequestParameterMap().get("game_code");
		if(Objects.isNull(gameCodeParam) || gameCodeParam.isEmpty() || StringUtils.isEmpty(gameCodeParam.get(0)))
		{
			SnakeGameService.handleNewGame(session);
		}
		else
		{
			SnakeGameService.handleJoinGame(gameCodeParam.get(0), session);
		}
	}

	@OnClose
	public void onClose(Session session, CloseReason closeReason) throws IOException
	{
		SnakeGameService.handlePlayerRemoval(session);
	}

	@OnMessage
	public void onMessage(Session session, String message) throws IOException
	{
		JSONObject data = new JSONObject(message);

		if(StringUtils.equals("keydown", data.getString("command")))
		{
			SnakeGameService.handleKeyDown(data.getJSONObject("data").getInt("key_code"), session);
		}
	}

	@OnError
	public void onError(Session session, Throwable throwable)
	{
		LOGGER.log(Level.SEVERE, "Error occurred for session " + session.getId(), throwable);
	}

}
