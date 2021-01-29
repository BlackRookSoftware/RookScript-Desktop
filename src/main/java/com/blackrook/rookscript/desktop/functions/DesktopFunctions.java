/*******************************************************************************
 * Copyright (c) 2021 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.desktop.functions;

import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.ScriptValue.Type;
import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.lang.ScriptFunctionUsage;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;
import com.blackrook.rookscript.resolvers.hostfunction.EnumFunctionResolver;

import static com.blackrook.rookscript.lang.ScriptFunctionUsage.type;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Script functions for the Desktop toolkit.
 * @author Matthew Tropiano
 */
public enum DesktopFunctions implements ScriptFunctionType
{
	OPEN(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Opens a file using the current OS-assigned program."
				)
				.parameter("file", 
					type(Type.STRING, "The path to the file to open."),
					type(Type.OBJECTREF, "File", "The path to the file to open.")
				)
				.returns(
					type(Type.NULL, "If [file] is null."),
					type(Type.BOOLEAN, "True if called, false if not."),
					type(Type.ERROR, "BadFile", "If the file is not found."),
					type(Type.ERROR, "NotSupported", "If opening a file is unsupported."),
					type(Type.ERROR, "IOError", "If the program cannot be started or a default program was not found."),
					type(Type.ERROR, "Security", "If the OS denies acces to the file or the program that opens that file.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				File file = popFile(scriptInstance, temp);
				
				if (!Desktop.isDesktopSupported())
				{
					returnValue.set(false);
					return true;
				}
				
				if (file == null)
				{
					returnValue.setNull();
					return true;
				}
				else if (!file.exists())
				{
					returnValue.setError("BadFile", "File does not exist: " + file.getPath());
					return true;
				}
				
				try {
					Desktop.getDesktop().open(file);
					returnValue.set(true);
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
				} catch (UnsupportedOperationException e) {
					returnValue.setError("NotSupported", e.getMessage(), e.getLocalizedMessage());
				} catch (SecurityException e) {
					returnValue.setError("Security", e.getMessage(), e.getLocalizedMessage());
				}
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	BROWSE(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Browses to a provided URI using the default browser."
				)
				.parameter("uri", 
					type(Type.STRING, "The URI to browse to.")
				)
				.returns(
					type(Type.NULL, "If [uri] is null."),
					type(Type.BOOLEAN, "True if browsed, false if not."),
					type(Type.ERROR, "BadURI", "If the URI is malformed."),
					type(Type.ERROR, "NotSupported", "If browsing to a URI is unsupported."),
					type(Type.ERROR, "IOError", "If the program cannot be started or a default program was not found."),
					type(Type.ERROR, "Security", "If the OS denies acces to the file or the program that opens that file.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				String address = temp.asString();
				
				if (address == null)
				{
					returnValue.setNull();
					return true;
				}
				
				if (!Desktop.isDesktopSupported())
				{
					returnValue.set(false);
					return true;
				}
				
				try {
					Desktop.getDesktop().browse(new URI(address));
					returnValue.set(true);
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
				} catch (UnsupportedOperationException e) {
					returnValue.setError("NotSupported", e.getMessage(), e.getLocalizedMessage());
				} catch (SecurityException e) {
					returnValue.setError("Security", e.getMessage(), e.getLocalizedMessage());
				} catch (URISyntaxException e) {
					returnValue.setError("BadURI", e.getMessage(), e.getLocalizedMessage());
				}
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	;
	
	private final int parameterCount;
	private Usage usage;
	private DesktopFunctions(int parameterCount)
	{
		this.parameterCount = parameterCount;
		this.usage = null;
	}
	
	/**
	 * @return a function resolver that handles all of the functions in this enum.
	 */
	public static final ScriptFunctionResolver createResolver()
	{
		return new EnumFunctionResolver(DesktopFunctions.values());
	}

	@Override
	public int getParameterCount()
	{
		return parameterCount;
	}

	@Override
	public Usage getUsage()
	{
		if (usage == null)
			usage = usage();
		return usage;
	}
	
	@Override
	public abstract boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue);

	protected abstract Usage usage();

	// Threadlocal "stack" values.
	private static final ThreadLocal<ScriptValue> CACHEVALUE1 = ThreadLocal.withInitial(()->ScriptValue.create(null));

	/**
	 * Pops a variable off the stack and, using a temp variable, extracts a File/String.
	 * @param scriptInstance the script instance.
	 * @param temp the temporary script value.
	 * @return a File object.
	 */
	private static File popFile(ScriptInstance scriptInstance, ScriptValue temp) 
	{
		scriptInstance.popStackValue(temp);
		if (temp.isNull())
			return null;
		else if (temp.isObjectRef(File.class))
			return temp.asObjectType(File.class);
		else
			return new File(temp.asString());
	}
	
}
