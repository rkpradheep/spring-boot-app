package com.server.framework.http;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class FormData
{
	boolean isFile;
	String value = StringUtils.EMPTY;
	String contentType;
	List<FileData> fileDataList = new ArrayList<>();

	public boolean isFile()
	{
		return isFile;
	}

	public void setIsFile(boolean file)
	{
		isFile = file;
	}

	public void addFileData(FileData fileData)
	{
		fileDataList.add(fileData);
	}

	public FileData getFileData()
	{
		return fileDataList.get(0);
	}

	public List<FileData> getFileDataList()
	{
		return fileDataList;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	public String getContentType()
	{
		return contentType;
	}

	public void setContentType(String contentType)
	{
		this.contentType = contentType;
	}

	public static class FileData
	{
		String fileName;
		String contentType;
		byte[] bytes;

		public FileData(String fileName, byte[] bytes, String contentType)
		{
			this.fileName = fileName;
			this.bytes = bytes;
			this.contentType = contentType;
		}

		public String getFileName()
		{
			return fileName;
		}

		public byte[] getBytes()
		{
			return bytes;
		}
		public InputStream getInputStream()
		{
			return new ByteArrayInputStream(bytes);
		}
		public Reader getReader()
		{
			return new InputStreamReader(getInputStream());
		}
		public String getContentType()
		{
			return contentType;
		}
	}
}
