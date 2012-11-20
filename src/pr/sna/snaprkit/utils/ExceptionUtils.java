package pr.sna.snaprkit.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtils
{
	public static String getExceptionStackString(Throwable e)
	{
		try
		{
		    StringWriter sw = new StringWriter();
		    PrintWriter pw = new PrintWriter(sw);
		    e.printStackTrace(pw);
		    return sw.toString();
		}
		catch(Exception e2)
		{
			return "bad stack2string";
		}
	}
}
