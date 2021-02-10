package com.blackrook.rookscript.desktop.functions;

import java.io.File;

import com.blackrook.rookscript.ScriptEnvironment;
import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.functions.CommonFunctions;
import com.blackrook.rookscript.functions.MathFunctions;
import com.blackrook.rookscript.functions.PrintFunctions;

public final class ImageTestMain 
{
	public static void main(String[] args) 
	{
		ScriptValue out = ScriptValue.create(null);
		ScriptInstance instance = ScriptInstance.createBuilder()
			.withEnvironment(ScriptEnvironment.createStandardEnvironment())
			.withSource(new File(args[0]))
			.withScriptStack(256, 1204)
			.withFunctionResolver(ImageFunctions.createResolver())
			.andFunctionResolver(DesktopFunctions.createResolver())
			.andFunctionResolver(CommonFunctions.createResolver())
			.andFunctionResolver(PrintFunctions.createResolver())
			.andFunctionResolver(MathFunctions.createResolver())
			.createInstance();
		instance.call("main");
		instance.popStackValue(out);
		System.out.println(out);
	}
}
