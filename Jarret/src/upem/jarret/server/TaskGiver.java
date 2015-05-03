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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TaskGiver {
	private final ArrayList<Task> tasks;

	public TaskGiver(List<Task> tasks) {
		this.tasks = (ArrayList<Task>) Objects.requireNonNull(tasks);
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

	public static TaskGiver create() throws IOException {
		ArrayList<String> strings = parseJsonData("workerdescription.json");

		// create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		objectMapper.setVisibilityChecker(VisibilityChecker.Std
				.defaultInstance().withFieldVisibility(
						JsonAutoDetect.Visibility.ANY));
		ArrayList<Task> jDatas = new ArrayList<Task>();
		// convert json string to object
		for (String s : strings) {
			jDatas.add(objectMapper.readValue(s, Task.class));
		}
		jDatas.stream().forEach(jd -> System.out.println(jd + "\n"));
		return new TaskGiver(jDatas);
	}

	public String giveJobByPriority() {
		Task t = tasks.stream().max((Task t1, Task t2) -> {
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
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode dataTable = mapper.createObjectNode();
			dataTable.put("ComeBackInSeconds", 5);
			return dataTable.toString();
		}
		return t.convertToJsonString();
	}
	
	public void taskGiven(String content){
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		try {
			JsonNode dataTable = objectMapper.readTree(content);
			String jobId = dataTable.get("JobId").textValue();
			String task = dataTable.get("Task").textValue();
			for(Task t : tasks){
				if(t.getJobId().equals(jobId) && t.getJobTaskNumber().equals(task)){
					System.out.println("Job : "+t.getJobId() + " task : " + t.getJobTaskNumber() + " given !!!!!");
					t.setTaskGiven();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
