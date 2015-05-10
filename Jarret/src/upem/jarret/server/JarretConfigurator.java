package upem.jarret.server;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;

public class JarretConfigurator {
	private static final String jarretConfiguratorPath = "JarRetConfig.json";
	private int Port;
	private String PathJobs;
	private int MaxSizeAnwser;
	private int TimeToSleep;
	private String PathLogs;
	
	public int getPort() {
		return Port;
	}

	public String getPathJobs() {
		return PathJobs;
	}

	public int getMaxSizeAnwser() {
		return MaxSizeAnwser;
	}

	public int getTimeToSleep() {
		return TimeToSleep;
	}
	
	public String getPathLogs() {
		return PathLogs;
	}

	public static JarretConfigurator create(){
		
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		objectMapper.setVisibilityChecker(VisibilityChecker.Std
				.defaultInstance().withFieldVisibility(
						JsonAutoDetect.Visibility.ANY));
		
		File f = new File(jarretConfiguratorPath);
	
		JarretConfigurator jarretConfigurator = null;
		try {
			jarretConfigurator = objectMapper.readValue(f, JarretConfigurator.class);
		} catch (IOException e) {
			System.err.println("Problem with file :" + jarretConfiguratorPath);
		}
		return jarretConfigurator;
		
	}
}
