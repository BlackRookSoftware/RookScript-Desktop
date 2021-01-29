package com.blackrook.rookscript.desktop.functions;

import java.io.File;

import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.functions.CommonFunctions;
import com.blackrook.rookscript.functions.MathFunctions;

public final class ImageTestMain 
{
	public static void main(String[] args) 
	{
		ScriptValue out = ScriptValue.create(null);
		ScriptInstance instance = ScriptInstance.createBuilder()
			.withSource(new File("src/test/resources/scripts/image.txt"))
			.withScriptStack(256, 1204)
			.withFunctionResolver(ImageFunctions.createResolver())
			.andFunctionResolver(DesktopFunctions.createResolver())
			.andFunctionResolver(CommonFunctions.createResolver())
			.andFunctionResolver(MathFunctions.createResolver())
			.createInstance();
		instance.call("main", (Object[])args);
		instance.popStackValue(out);
		System.out.println(out);
	}
}
