package com.nurgak.trebla.services;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

public class Command
{
	public String command = "";
	public String argument = "";
	Map<String, String> flags = new HashMap<String, String>();
	
	Pattern partsPattern = Pattern.compile("^([^\\s]+)(?:\\s+-.+?)?\\s*?([^\\s-]+)?$");
	Pattern paramsPattern = Pattern.compile("-{1,2}([^\\s=]+)(?:=([^\"\\s]+|\"[^\"]+\"))?");
	Matcher partsMatcher, paramsMatcher;

	public Command(String input)
	{
		if(input == null || input.length() == 0)
		{
			Log.d("trebla", "Null input detected");
			return;
		}
		
		partsMatcher = partsPattern.matcher(input);
		
		if(!partsMatcher.find())
		{
			Log.d("trebla", "Command did not match pattern");
			return;
		}
		
		command = partsMatcher.group(1);
		Log.d("trebla", "Command: " + partsMatcher.group(1));
		
		// commands do not have to have an argument
		if(partsMatcher.group(2) != null)
		{
			argument = partsMatcher.group(2);
			Log.d("trebla", "Argument: " + partsMatcher.group(2));
		}
		
		// flags
		paramsMatcher = paramsPattern.matcher(input);
		while(paramsMatcher.find())
		{
			flags.put(paramsMatcher.group(1), paramsMatcher.group(2));
			Log.d("trebla", "Param: " + paramsMatcher.group(1) + " = " + paramsMatcher.group(2));
		}
	}

	public String getFlag(String key)
	{
		if(flags.containsKey(key))
		{
			return flags.get(key);
		}
		return null;
	}

	public boolean checkFlag(String key)
	{
		if(flags.containsKey(key))
		{
			return true;
		}
		return false;
	}
}