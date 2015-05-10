package upem.jarret.server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.esotericsoftware.minlog.Log;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TaskGiver {
	private static final String taskLocation = "workerdescription.json";
	private final ArrayList<ServerTask> tasks;
	private final int comeBackInSeconds;

	public TaskGiver(List<ServerTask> tasks, int comeBackInSeconds) {
		this.tasks = (ArrayList<ServerTask>) Objects.requireNonNull(tasks);
		this.comeBackInSeconds = comeBackInSeconds;
	}

	/**
	 * Parse a file that contains several task to complete
	 * @param path of the file to parse
	 * @return a list of all tasks parsed
	 * @throws IOException
	 */
	private static ArrayList<String> parseJsonData(String path)
			throws IOException {
		FileInputStream fstream = new FileInputStream(path);
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;
		StringBuilder sb = new StringBuilder();
		ArrayList<String> strings = new ArrayList<>();

		// Read File Line By Line
		while ((strLine = br.readLine()) != null) {
			sb.append(strLine);
			if (strLine.equals("")) {
				strLine = sb.toString();
				strings.add(strLine);
				sb = new StringBuilder();
			}
		}
		strLine = sb.toString();
		strings.add(strLine);
		br.close();
		return strings;
	}

	/**
	 * Create a TaskGiver that gives task to the client
	 * @param comeBackInSeconds number of seconds to wait if there is no task to send
	 * @return a TaskGiver
	 * @throws IOException
	 */
	public static TaskGiver create(int comeBackInSeconds) throws IOException {
		ArrayList<String> strings = parseJsonData(taskLocation);

		// create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		objectMapper.setVisibilityChecker(VisibilityChecker.Std
				.defaultInstance().withFieldVisibility(
						JsonAutoDetect.Visibility.ANY));
		ArrayList<ServerTask> jDatas = new ArrayList<ServerTask>();
		// convert json string to object
		for (String s : strings) {
			jDatas.add(objectMapper.readValue(s, ServerTask.class));
		}
		Log.info("Loading all task from " + taskLocation);
		return new TaskGiver(jDatas, comeBackInSeconds);
	}

	/**
	 * Return a task by priority that needs to be complete.
	 * @return JSON String of incomplete task.
	 * @throws JsonProcessingException
	 */
	public String giveJobByPriority() throws JsonProcessingException {
		ServerTask t = tasks.stream().max((ServerTask t1, ServerTask t2) -> {
			 if (t1.getPriority() > t2.getPriority()) {
				return 1;
			} else
				return -1;
		}).get();

		// If all tasks has given
		if (t.getPriority() == 0) {
			Log.warn("No more task are available");
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode dataTable = mapper.createObjectNode();
			dataTable.put("ComeBackInSeconds", comeBackInSeconds);
			Log.warn("No Task available for the momment, client has to come back in " + comeBackInSeconds + " sec" );
			return mapper.writeValueAsString(dataTable);
		}
		t.decreasePriority();
		Log.info("Find task " + t.getJobTaskNumber() + " of job "  + t.getJobId() + " for the client");
		return t.convertToJsonString();
	}
	
	/**
	 * Display information about all task
	 */
	public void info(){
		tasks.stream().forEach(
				t -> Log.trace("JobId: " + t.getJobId() + "    "
						+ "JobTaskNumber: " + t.getJobTaskNumber() + "    "
						+ "JobPriority: " + t.getPriority()));
	}

}
