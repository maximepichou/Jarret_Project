package upem.jarret.server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
		System.out.println("Loading all task from " + taskLocation);
		return new TaskGiver(jDatas, comeBackInSeconds);
	}

	public String giveJobByPriority() throws JsonProcessingException {
		ServerTask t = tasks.stream().max((ServerTask t1, ServerTask t2) -> {
			if (t1.taskGiven() && !t2.taskGiven()) {
				return -1;
			} else if (!t1.taskGiven() && t2.taskGiven()) {
				return 1;
			} else if (t1.getPriority() > t2.getPriority()) {
				return 1;
			} else
				return -1;
		}).get();

		// If all tasks has given
		if (t.taskGiven()) {
			System.out.println("No more task are available");
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode dataTable = mapper.createObjectNode();
			dataTable.put("ComeBackInSeconds", comeBackInSeconds);
			return mapper.writeValueAsString(dataTable);
		}
		System.out.println("Find task " + t.getJobTaskNumber() + " of job "  + t.getJobId() + " for the client");
		return t.convertToJsonString();
	}

	public void taskGiven(String content) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		try {
			JsonNode dataTable = objectMapper.readTree(content);
			String jobId = dataTable.get("JobId").textValue();
			String task = dataTable.get("Task").textValue();
			for (ServerTask t : tasks) {
				if (t.getJobId().equals(jobId)
						&& t.getJobTaskNumber().equals(task)) {
					t.setTaskGiven();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
