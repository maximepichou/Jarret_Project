package fr.upem.logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import com.esotericsoftware.minlog.Log;
import com.esotericsoftware.minlog.Log.Logger;

public class MyLogger extends Logger{
	
	private final PrintStream outInfos;
	private final PrintStream outWarning;
	private final PrintStream outError;
	private final SimpleDateFormat dateFormatLog = new SimpleDateFormat(
			"HH:mm:ss");
	
	public MyLogger(String logPath) throws IOException {
		File dir = new File(logPath);
		if (!dir.exists()) {
			if (!dir.mkdir()) {
				throw new IOException("Cannot create "+ logPath+" directory");
			}
		}
		File infoFile = new File(logPath+"/logInfo.log");
		if (!infoFile.exists()) {
			if (!infoFile.createNewFile()) {
				throw new IOException("Cannot create logInfo file");
			}
		}
		File warningFile = new File(logPath+"/logWarning.log");
		if (!warningFile.exists()) {
			if (!warningFile.createNewFile()) {
				throw new IOException("Cannot create logWarning file");
			}
		}
		File errorFile = new File(logPath+"/logError.log");
		if (!errorFile.exists()) {
			if (!errorFile.createNewFile()) {
				throw new IOException("Cannot create logError file");
			}
		}
		this.outInfos = new PrintStream(infoFile);
		this.outWarning = new PrintStream(warningFile);
		this.outError = new PrintStream(errorFile);
	}
	
	public void log (int level, String category, String message, Throwable ex) {
        StringBuilder builder = new StringBuilder(256);
        builder.append(dateFormatLog.format(Calendar.getInstance().getTime()));
        builder.append(' ');
        builder.append('[');
        switch (level) {
		case Log.LEVEL_ERROR:
			builder.append("ERROR");
			break;
		case Log.LEVEL_WARN:
			builder.append("WARN");
			break;
		case Log.LEVEL_INFO:
			builder.append("INFO");
			break;
		case Log.LEVEL_TRACE:
			builder.append("TRACE");
			break;
		}
        builder.append("] : ");
        builder.append(message);
        if (ex != null) {
            StringWriter writer = new StringWriter(256);
            ex.printStackTrace(new PrintWriter(writer));
            builder.append('\n');
            builder.append(writer.toString().trim());
        }
        if(level == Log.LEVEL_ERROR){
        	outError.println(builder);
        }
        if(level == Log.LEVEL_WARN){
        	outWarning.println(builder);
        }
        if(level == Log.LEVEL_INFO){
        	outInfos.println(builder);
        }
        
        System.out.println(builder);
    }
}

