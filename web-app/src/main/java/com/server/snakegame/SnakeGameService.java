package com.server.snakegame;

import jakarta.websocket.Session;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONObject;

public class SnakeGameService
{
	private static final Integer FRAME_RATE = 5;
	private static final Integer GRID_SIZE = 20;
	private static final Map<String, RoomMeta> NAME_ROOM_META = new ConcurrentHashMap<>();
	private static final Map<String, String> SESSION_ID_ROOM_NAME = new ConcurrentHashMap<>();
	private static final List<String> SNAKE_COLOR_LIST = Arrays.asList("red", "green", "yellow", "blue");
	private static final Integer PLAYER_LIMIT = SNAKE_COLOR_LIST.size() + 1;
	private static final Logger LOGGER = Logger.getLogger(SnakeGameService.class.getName());
	private static final ScheduledThreadPoolExecutor SCHEDULED_THREAD_POOL_EXECUTOR = new ScheduledThreadPoolExecutor(1);

	static
	{
		SCHEDULED_THREAD_POOL_EXECUTOR.setRemoveOnCancelPolicy(true);
	}

	protected static void handleNewGame(Session session) throws IOException
	{
		JSONObject payload = new JSONObject();

		String roomName = RandomStringUtils.randomNumeric(5);

		RoomMeta roomMeta = new RoomMeta();
		roomMeta.addPlayerSession(session, 1);

		NAME_ROOM_META.put(roomName, roomMeta);
		SESSION_ID_ROOM_NAME.put(session.getId(), roomName);

		session.getBasicRemote().sendText(payload.put("command", "gameCode").put("data", new JSONObject().put("game_code", roomName)).toString());

		String playerName = session.getRequestParameterMap().get("name").get(0);

		State state = new State();
		state.gridsize = GRID_SIZE;
		state.players.add(getNewPlayer(1, playerName));
		randomFood(state);

		roomMeta.setState(state);

		payload = new JSONObject();
		session.getBasicRemote().sendText(payload.put("command", "init").put("data", new JSONObject().put("number", 1)).toString());

		startGameInterval(roomName);
	}

	protected static void handleJoinGame(String roomName, Session session) throws IOException
	{
		RoomMeta roomMeta = NAME_ROOM_META.get(roomName);

		JSONObject payload = new JSONObject();

		if(Objects.isNull(roomMeta))
		{
			session.getBasicRemote().sendText(payload.put("command", "unknownCode").toString());
			return;
		}

		int playerCount = roomMeta.getPlayerCount();

		if(playerCount == PLAYER_LIMIT)
		{
			session.getBasicRemote().sendText(payload.put("command", "tooManyPlayers").toString());
			return;
		}

		int playerNumber = roomMeta.getPlayerSessionCount() + 1;

		roomMeta.addPlayerSession(session, playerNumber);


		State state = roomMeta.getState();

		state.players.add(getNewPlayer(playerNumber, session.getRequestParameterMap().get("name").get(0)));

		SESSION_ID_ROOM_NAME.put(session.getId(), roomName);

		session.getBasicRemote().sendText(payload.put("command", "init").put("data", new JSONObject().put("number", playerNumber)).toString());
	}

	static State.Player getNewPlayer(int playerNumber, String playerName)
	{

		int x = (int) Math.floor(Math.random() * GRID_SIZE);
		x = x < 2 ? x += 2 : x;
		int y = (int) Math.floor(Math.random() * GRID_SIZE);

		State.Player player = new State.Player();
		player.pos = new State.Player.Pos(x, y);
		player.vel = new State.Player.Vel(1, 0);
		player.snake.addLast(new State.Player.Pos(x, y));
		player.snake.addLast(new State.Player.Pos(x - 1, y));
		player.snake.addLast(new State.Player.Pos(x - 2, y));
		player.color = SNAKE_COLOR_LIST.get(playerNumber - 1);
		player.number = playerNumber;
		player.name = playerName;

		return player;

	}

	static void handlePlayerRemoval(Session session) throws IOException
	{
		String roomName = SESSION_ID_ROOM_NAME.get(session.getId());

		RoomMeta roomMeta = NAME_ROOM_META.get(roomName);
		if(Objects.isNull(roomMeta))
		{
			return;
		}
		roomMeta.markPlayerAsEliminated(session);

		if(roomMeta.getActivePlayerCount() == 1)
		{
			emitResult(roomName);
			roomMeta.destroy();
			NAME_ROOM_META.remove(roomName);
		}
		else if(roomMeta.getActivePlayerCount() == 0)
		{
			roomMeta.destroy();
			NAME_ROOM_META.remove(roomName);
		}
	}

	static void handleKeyDown(int keyCode, Session session)
	{

		String roomName = SESSION_ID_ROOM_NAME.get(session.getId());
		int playerNumber = NAME_ROOM_META.get(roomName).getPlayerNumberFromSession(session);

		Optional<State.Player> playerOptional = NAME_ROOM_META.get(roomName).getState().players.stream().filter(player -> player.number == playerNumber).findFirst();
		playerOptional.get().vel = getUpdatedVelocity(keyCode);

	}

	static void startGameInterval(String roomName)
	{

		Runnable runnable = () -> {
			try
			{
				RoomMeta roomMeta = NAME_ROOM_META.get(roomName);
				if(Objects.isNull(roomMeta))
				{
					return;
				}

				State state = roomMeta.getState();

				int loser = gameLoop(state);

				if(loser == 0)
				{
					emitGameState(roomName, state);
				}
				else
				{
					emitGameOver(roomName, loser);
					handlePlayerRemoval(roomMeta.getSessionFromPlayerNumber(loser));
				}

			}
			catch(Exception e)
			{
				LOGGER.log(Level.SEVERE, "Exception", e);
			}
		};
		ScheduledFuture<?> scheduledFuture = SCHEDULED_THREAD_POOL_EXECUTOR.scheduleWithFixedDelay(runnable, 0, 1000 / FRAME_RATE, TimeUnit.MILLISECONDS);

		NAME_ROOM_META.get(roomName).setStateUpdateTask(scheduledFuture);
	}

	static void emitGameState(String room, State gameState) throws IOException
	{
		for(Session session : NAME_ROOM_META.get(room).getPlayerSessionList())
		{
			session.getBasicRemote().sendText(new JSONObject().put("command", "gameState").put("data", gameState.toJSON()).toString());
		}
	}

	static void emitGameOver(String room, int loser) throws IOException
	{
		Session session = NAME_ROOM_META.get(room).getSessionFromPlayerNumber(loser);
		session.getBasicRemote().sendText(new JSONObject().put("command", "gameOver").toString());
	}

	static void emitResult(String room) throws IOException
	{
		JSONObject resultJSON = new JSONObject();
		resultJSON.put("command", "showResult");
		resultJSON.put("data", NAME_ROOM_META.get(room).getState().toJSON().put("winner", NAME_ROOM_META.get(room).getWinner()));
		for(Session session : NAME_ROOM_META.get(room).getPlayerSessionList())
		{
			try
			{
				session.getBasicRemote().sendText(resultJSON.toString());
			}
			catch(Exception e)
			{

			}
		}
	}

	private static int gameLoop(State state)
	{
		for(State.Player player : state.players)
		{
			if(player.isEliminated())
			{
				continue;
			}
			player.pos.x += player.vel.x;
			player.pos.y += player.vel.y;

			if(player.pos.x < 0)
			{
				player.pos.x = GRID_SIZE - 1;
			}
			if(player.pos.x >= GRID_SIZE)
			{
				player.pos.x = 0;
			}
			if(player.pos.y < 0)
			{
				player.pos.y = GRID_SIZE - 1;
			}
			if(player.pos.y >= GRID_SIZE)
			{
				player.pos.y = 0;
			}

			if(state.food.x == player.pos.x && state.food.y == player.pos.y)
			{
				player.snake.addFirst(new State.Player.Pos(player.pos.x, player.pos.y));
				player.pos.x += player.vel.x;
				player.pos.y += player.vel.y;
				player.points += 10;
				randomFood(state);
			}

			for(State.Player.Pos cell : player.snake)
			{
				if(cell.x == player.pos.x && cell.y == player.pos.y)
				{
					return player.number;
				}
			}

			player.snake.addFirst(new State.Player.Pos(player.pos.x, player.pos.y));
			player.snake.removeLast();

		}

		return 0;
	}

	static void randomFood(State state)
	{
		State.Food food = new State.Food();
		food.x = (int) Math.floor(Math.random() * GRID_SIZE);
		food.y = (int) Math.floor(Math.random() * GRID_SIZE);

		for(State.Player player : state.players)
		{
			for(State.Player.Pos cell : player.snake)
			{
				if(cell.x == food.x && cell.y == food.y)
				{
					randomFood(state);
				}
			}
		}

		state.food = food;
	}

	private static State.Player.Vel getUpdatedVelocity(int keyCode)
	{
		switch(keyCode)
		{
			case 37 ->
			{ // left
				return new State.Player.Vel(-1, 0);
			}
			case 38 ->
			{ // down
				return new State.Player.Vel(0, -1);
			}
			case 39 ->
			{ // right
				return new State.Player.Vel(1, 0);
			}
			case 40 ->
			{ // up
				return new State.Player.Vel(0, 1);
			}
		}
		return null;
	}

}
