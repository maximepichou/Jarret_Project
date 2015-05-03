package upem.jarret.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class ServerTask {
	private String JobId;
	private String JobTaskNumber;
	private String JobDescription;
	private String JobPriority;
	private String WorkerVersionNumber;
	private String WorkerURL;
	private String WorkerClassName;
	private boolean given = false;
	
	
	public String getJobId(){
		return JobId;
	}
	
	public String getJobTaskNumber(){
		return JobTaskNumber;
	}
	
	public int getPriority(){
		return Integer.valueOf(JobPriority);
	}
	
	public boolean taskGiven(){
		return given;
	}
	
	public void setTaskGiven(){
		given = true;
	}

	public String convertToJsonString()  {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		ObjectNode dataTable = mapper.createObjectNode();
		dataTable.put("JobId", JobId);
		dataTable.put("WorkerVersion", WorkerVersionNumber);
		dataTable.put("WorkerURL", WorkerURL);
		dataTable.put("WorkerClassName", WorkerClassName);
		dataTable.put("Task", JobTaskNumber);
		String result = null;
		try {
			result = mapper.writeValueAsString(dataTable);
		} catch (JsonProcessingException e1) {
			e1.printStackTrace();
		}
		
		return result;
	}
	
	@Override
	public String toString(){
	StringBuilder sb = new StringBuilder();
	
	sb.append("\"JobId\" : \""+JobId+"\",\n");
	sb.append("\"JobTaskNumber\" : \""+JobTaskNumber+"\",\n");
	sb.append("\"JobDescription\" : \""+JobDescription+"\",\n");
	sb.append("\"JobPriority\" : \""+JobPriority+"\",\n");
	sb.append("\"WorkerVersionNumber\" : \""+WorkerVersionNumber+"\",\n");
	sb.append("\"WorkerURL\" : \""+WorkerURL+"\",\n");
	sb.append("\"WorkerClassName\" : \""+WorkerClassName+"\",\n");
	sb.append("\"given\" : \""+given+"\",\n");
	return sb.toString();
	}
}
