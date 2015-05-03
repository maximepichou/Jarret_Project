package upem.jarret.client;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.Iterator;

import upem.jarret.worker.Worker;

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

	

	/*public ClientTask(String jobId, String workerVersion, String workerURL,
			String workerClassName, String task) {

		this.JobId = Objects.requireNonNull(jobId);
		this.WorkerVersion = Objects.requireNonNull(workerVersion);
		this.WorkerURL = Objects.requireNonNull(workerURL);
		this.WorkerClassName = Objects.requireNonNull(workerClassName);
		this.Task = Objects.requireNonNull(task);
		ComeBackInSeconds = 0;
	}

	public ClientTask(int secondsToSleep) {
		if (secondsToSleep <= 0) {
			throw new IllegalArgumentException();
		}
		this.ComeBackInSeconds = secondsToSleep;
	}*/

	public int haveToSleep() {
		return ComeBackInSeconds;
	}

	public static ClientTask create(String content) throws IOException {
		System.out.println(content);
		
		// create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		objectMapper.setVisibilityChecker(VisibilityChecker.Std
				.defaultInstance().withFieldVisibility(
						JsonAutoDetect.Visibility.ANY));
		ClientTask ct = objectMapper.readValue(content, ClientTask.class);
		return ct;
		/*JsonFactory jfactory = new JsonFactory();
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
				task);*/
		
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
		Worker worker = WorkerFactory.getWorker(WorkerURL, WorkerClassName);
		System.out.println("Computing");
		try {
			ObjectMapper mapper = new ObjectMapper();
			Answer = mapper.readTree(worker.compute(Integer.valueOf(Task)));
			checkAnswer();
		} catch (Exception e) {
			Error = "Computation error";
			System.err.println("Error while computing");
		}
		System.out.println("Computed");
	}

	public void checkAnswer() {
		if(!isValidJSON(Answer.toString())){
			Error = "Answer is not valid JSON";
		}
		else if(isJSONContainsObject(Answer.toString())){
			Error = "Answer is nested";
		}
		else if(Answer.size()>4096){
			Error = "Too Long";
		}
	}

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
			if(Error.equals("")){
			dataTable.set("Answer", Answer);
			}
			else{
				dataTable.put("Error", Error);
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
		sb.append("\"JobId\" : \"" + JobId + "\",\n");
		sb.append("\"WorkerVersion\" : \"" + WorkerVersion + "\",\n");
		sb.append("\"WorkerURL\" : \"" + WorkerURL + "\",\n");
		sb.append("\"WorkerClassName\" : \"" + WorkerClassName + "\",\n");
		sb.append("\"Task\" : \"" + Task + "\",\n");
		sb.append("\"Answer\" : \"" + Answer + "\"\n");
		sb.append("}");
		return sb.toString();
	}

}
