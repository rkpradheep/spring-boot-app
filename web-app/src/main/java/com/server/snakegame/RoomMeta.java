package com.server.snakegame;

import jakarta.websocket.Session;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

public class RoomMeta
{
	private State state;
	private ScheduledFuture<?> stateUpdateTask;
	private final Map<Session, Integer> sessionPlayerNumberMap = new ConcurrentHashMap<>();

	public State getState()
	{
		return state;
	}

	public void setState(State state)
	{
		this.state = state;
	}

	public void setStateUpdateTask(ScheduledFuture<?> stateUpdateTask)
	{
		this.stateUpdateTask = stateUpdateTask;
	}

	public Session getSessionFromPlayerNumber(int playerNumber)
	{
		for(Map.Entry<Session, Integer> sessionIntegerEntry : sessionPlayerNumberMap.entrySet())
		{
			if(sessionIntegerEntry.getValue() == playerNumber)
				return sessionIntegerEntry.getKey();
		}
		return null;
	}

	public int getPlayerNumberFromSession(Session session)
	{
		return sessionPlayerNumberMap.get(session);
	}

	public void addPlayerSession(Session session, int playerNumber)
	{
		this.sessionPlayerNumberMap.put(session, playerNumber);
	}

	public int getPlayerSessionCount()
	{
		return this.sessionPlayerNumberMap.keySet().size();
	}

	public void markPlayerAsEliminated(Session session)
	{
		int playerNumber = sessionPlayerNumberMap.get(session);
		Optional<State.Player> playerOptional = state.players.stream().filter(player -> player.number == playerNumber).findFirst();
		playerOptional.ifPresent(State.Player::eliminated);
	}

	public void cancelStateUpdateTask()
	{
		if(Objects.nonNull(stateUpdateTask))
		{
			stateUpdateTask.cancel(true);
			stateUpdateTask = null;
		}
	}

	public int getPlayerCount()
	{
		return state.players.size();
	}

	public int getActivePlayerCount()
	{
		long eliminatedCount = state.players.stream().filter(State.Player::isEliminated).count();
		return (int) (getPlayerCount() - eliminatedCount);
	}

	public List<Session> getPlayerSessionList()
	{
		return new ArrayList<>(sessionPlayerNumberMap.keySet());
	}

	public String getWinner()
	{
		Optional<State.Player> playerOptional = state.players.stream().max(Comparator.comparingInt(State.Player::getPoints));
		return playerOptional.get().name;
	}

	public void destroy()
	{
		cancelStateUpdateTask();
		this.stateUpdateTask = null;
		this.sessionPlayerNumberMap.clear();
		this.state = null;
	}

}
