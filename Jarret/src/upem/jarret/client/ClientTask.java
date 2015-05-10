package upem.jarret.client;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.Iterator;

import upem.jarret.worker.Worker;

import com.esotericsoftware.minlog.Log;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ClientTask {
	private String JobId;
	private String WorkerVersion;
	private String WorkerURL;
	private String WorkerClassName;
	private String Task;
	private String ClientId;
	private JsonNode Answer;
	private String Error = "";
	private int ComeBackInSeconds = 0;

	public String getJobId() {
		return JobId;
	}

	public String getWorkerVersion() {
		return WorkerVersion;
	}

	public String getWorkerURL() {
		return WorkerURL;
	}

	public String getWorkerClassName() {
		return WorkerClassName;
	}

	public String getTask() {
		return Task;
	}

	public String getClientId() {
		return ClientId;
	}

	public JsonNode getAnswer() {
		return Answer;
	}

	public String getError() {
		return Error;
	}

	public int getComeBackInSeconds() {
		return ComeBackInSeconds;
	}
	
	public int haveToSleep() {
		return ComeBackInSeconds;
	}

	/**
	 * Create a new ClientTask that contains the task to solve.
	 * @param content JSON string that describe the task to solve.
	 * @return new ClientTask.
	 * @throws IOException
	 */
	public static ClientTask create(String content) throws IOException {
		// create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		objectMapper.setVisibilityChecker(VisibilityChecker.Std
				.defaultInstance().withFieldVisibility(
						JsonAutoDetect.Visibility.ANY));
		ClientTask ct = objectMapper.readValue(content, ClientTask.class);
		return ct;
	}

	/**
	 * Check if the JSON String is a valid JSON.
	 * @param JSON String to check if it valid JSON.
	 * @return true if it valid, false otherwise.
	 */
	public boolean isValidJSON(final String json) {
		boolean valid = true;
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.readTree(json);
		} catch (JsonParseException jpe) {
			valid = false;
		} catch (IOException ioe) {
			valid = false;
		}

		return valid;
	}

	/**
	 * Check if the JSON String contains an Object JSON.
	 * @param json JSON String to check.
	 * @return true if it contains, false otherwise.
	 */
	public boolean isJSONContainsObject(String json) {
		JsonNode arrNode;
		try {
			arrNode = new ObjectMapper().readTree(json);
			Iterator<JsonNode> it = arrNode.iterator();
			JsonNode jn;
			while (it.hasNext()) {
				jn = it.next();
				if (jn.isObject()) {
					return true;
				}
			}

		} catch (IOException e) {
			return false;
		}
		return false;
	}

	/**
	 * Compute the task with the worker.
	 * @throws InvocationTargetException
	 * @throws MalformedURLException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public void doWork() throws InvocationTargetException,
			MalformedURLException, ClassNotFoundException,
			IllegalAccessException, InstantiationException {
		Log.info("Loading Class ...");
		Worker worker = WorkerFactory.getWorker(WorkerURL, WorkerClassName);
		Log.info("Working in progress ...");
		try {
			ObjectMapper mapper = new ObjectMapper();
			Answer = mapper.readTree(worker.compute(Integer.valueOf(Task)));
			checkAnswer();
		} catch (Exception e) {
			Error = "Computation error";
			Log.warn("Error while computing");
		}
		Log.info("Task is over");
	}

	/**
	 * Check if the answer is valid or contains error.
	 */
	public void checkAnswer() {
		if (!isValidJSON(Answer.toString())) {
			Error = "Answer is not valid JSON";
		} else if (isJSONContainsObject(Answer.toString())) {
			Error = "Answer is nested";
		} else if (Answer.size() > 4096) {
			Error = "Too Long";
		}
	}

	/**
	 * Convert ClientTask object into JSON String.
	 * @param clientId the id of the client.
	 * @return JSON String of a ClientTask.
	 */
	public String convertToJsonString(String clientId) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		ObjectNode dataTable = mapper.createObjectNode();
		String result = null;
		dataTable.put("JobId", JobId);
		dataTable.put("WorkerVersion", WorkerVersion);
		dataTable.put("WorkerURL", WorkerURL);
		dataTable.put("WorkerClassName", WorkerClassName);
		dataTable.put("Task", Task);
		dataTable.put("ClientId", clientId);

		try {
			if (Error.equals("")) {
				dataTable.set("Answer", Answer);
			} else {
				dataTable.put("Error", Error);
			}
			result = mapper.writeValueAsString(dataTable);
		} catch (IOException e) {
			Log.error("", e);
		}
		return result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Task " + Task + " of Job ");
		sb.append(" JobId " + JobId );
		return sb.toString();
	}

}
