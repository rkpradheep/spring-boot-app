package com.server.snakegame;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.json.JSONObject;

import com.server.framework.common.CommonService;

public class State
{
	List<Player> players = new ArrayList<>();
	Food food;
	int gridsize;

	public JSONObject toJSON()
	{
		return CommonService.covertPOJOToJSON(this, State.class);
	}

	public static class Player
	{
		boolean eliminated;

		String name;
		String color;
		Pos pos;
		Vel vel;
		Deque<Pos> snake = new ArrayDeque<>();
		int number;
		int points;

		public int getPoints()
		{
			return points;
		}

		public void eliminated()
		{
			this.eliminated = true;
		}

		public boolean isEliminated()
		{
			return eliminated;
		}

		public static class Vel
		{
			public Vel(int x, int y)
			{
				this.x = x;
				this.y = y;
			}

			int x;
			int y;
		}

		public static class Pos
		{
			public Pos()
			{

			}

			public Pos(int x, int y)
			{
				this.x = x;
				this.y = y;
			}

			int x;
			int y;
		}
	}

	public static class Food
	{
		int x;
		int y;
	}
}
