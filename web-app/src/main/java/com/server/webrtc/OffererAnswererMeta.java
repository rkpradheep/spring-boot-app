package com.server.webrtc;

import jakarta.websocket.Session;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

public class OffererAnswererMeta
{
	public JSONObject offer;
	private List<JSONObject> offererCandidateList = new ArrayList<>();
	private Session answererSession;
	private Session offererSession;

	public JSONObject getOffer()
	{
		return offer;
	}

	public void setOffer(JSONObject offer)
	{
		this.offer = offer;
	}

	public List<JSONObject> getOffererCandidateList()
	{
		return offererCandidateList;
	}

	public void addToOffererCandidateList(JSONObject offererCandidate)
	{
		this.offererCandidateList.add(offererCandidate);
	}

	public Session getAnswererSession()
	{
		return answererSession;
	}

	public void setAnswererSession(Session answererSession)
	{
		this.answererSession = answererSession;
	}

	public Session getOffererSession()
	{
		return offererSession;
	}

	public void setOffererSession(Session offererSession)
	{
		this.offererSession = offererSession;
	}
}
