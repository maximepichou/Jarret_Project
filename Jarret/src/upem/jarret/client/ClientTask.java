package upem.jarret.client;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Objects;

import upem.jarret.worker.Worker;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ClientTask {
	private String jobId;
	private String workerVersion;
	private String workerURL;
	private String workerClassName;
	private String task;
	private String answer;
	private String error = "";
	private int secondsToSleep;

	public ClientTask(String jobId, String workerVersion, String workerURL,
			String workerClassName, String task) {

		this.jobId = Objects.requireNonNull(jobId);
		this.workerVersion = Objects.requireNonNull(workerVersion);
		this.workerURL = Objects.requireNonNull(workerURL);
		this.workerClassName = Objects.requireNonNull(workerClassName);
		this.task = Objects.requireNonNull(task);
		secondsToSleep = 0;
	}

	public ClientTask(int secondsToSleep) {
		if (secondsToSleep <= 0) {
			throw new IllegalArgumentException();
		}
		this.secondsToSleep = secondsToSleep;
	}

	public int haveToSleep() {
		return secondsToSleep;
	}

	public static ClientTask create(ByteBuffer buff, HTTPHeader header) {
		if (!"application/json".equals(header.getContentType())) {
			throw new IllegalArgumentException("This is not JSON Content-Type");
		}
		String content = header.getCharset().decode(buff).toString();
		System.out.println(content);
		JsonFactory jfactory = new JsonFactory();
		String jobId = "";
		String workerVersion = "";
		String workerURL = "";
		String workerClassName = "";
		String task = "";
		try {
			JsonParser jParser = jfactory.createParser(content);
			jParser.nextToken();
			// loop until token equal to "}"
			while (jParser.nextToken() != JsonToken.END_OBJECT) {

				String fieldname = jParser.getCurrentName();
				switch (fieldname) {
				case "JobId":
					jParser.nextToken();
					jobId = jParser.getText();
					break;
				case "WorkerVersion":
					jParser.nextToken();
					workerVersion = jParser.getText();
					break;
				case "WorkerURL":
					jParser.nextToken();
					workerURL = jParser.getText();
					break;
				case "WorkerClassName":
					jParser.nextToken();
					workerClassName = jParser.getText();
					break;
				case "Task":
					jParser.nextToken();
					task = jParser.getText();
					break;
				case "ComeBackInSeconds":
					return new ClientTask(jParser.getIntValue());

				}
			}
			jParser.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		if (jobId.equals("") || task.equals("") || workerVersion.equals("")
				|| workerURL.equals("") || workerURL.equals(""))
			throw new IllegalStateException();
		return new ClientTask(jobId, workerVersion, workerURL, workerClassName,
				task);
	}

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	public void doWork() throws InvocationTargetException,
			MalformedURLException, ClassNotFoundException,
			IllegalAccessException, InstantiationException {
		Worker worker = WorkerFactory.getWorker(workerURL, workerClassName);
		System.out.println("Computing");
		try {
			answer = worker.compute(Integer.valueOf(task));
			checkAnswer();
		} catch (Exception e) {
			error = "Computation error";
			System.err.println("Error while computing");
		}
		System.out.println("Computed");
	}

	public void checkAnswer() {
		if(!isValidJSON(answer)){
			error = "Answer is not valid JSON";
		}
		else if(isJSONContainsObject(answer)){
			error = "Answer is nested";
		}
		else if(answer.length()>4096){
			error = "Too Long";
		}
	}

	public String convertToJsonString(String clientId) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		ObjectNode dataTable = mapper.createObjectNode();
		String result = null;
		dataTable.put("JobId", jobId);
		dataTable.put("WorkerVersion", workerVersion);
		dataTable.put("WorkerURL", workerURL);
		dataTable.put("WorkerClassName", workerClassName);
		dataTable.put("Task", task);
		dataTable.put("ClientId", clientId);

		try {
			if(error.equals("")){
			dataTable.set("Answer", mapper.readTree(answer));
			}
			else{
				dataTable.set("Error", mapper.readTree(error));
			}
			result = mapper.writeValueAsString(dataTable);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		sb.append("\"JobId\" : \"" + jobId + "\",\n");
		sb.append("\"WorkerVersion\" : \"" + workerVersion + "\",\n");
		sb.append("\"WorkerURL\" : \"" + workerURL + "\",\n");
		sb.append("\"WorkerClassName\" : \"" + workerClassName + "\",\n");
		sb.append("\"Task\" : \"" + task + "\",\n");
		sb.append("\"Answer\" : \"" + answer + "\"\n");
		sb.append("}");
		return sb.toString();
	}

}
