package upem.jarret.server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;

public class JsonData {
	private String JobId;
	private String JobTaskNumber;
	private String JobDescription;
	private String JobPriority;
	private String WorkerVersionNumber;
	private String WorkerURL;
	private String WorkerClassName;

	public static JsonData create() throws IOException {
		FileInputStream fstream = new FileInputStream("workerdescription.json");
		// or using Scaner
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;
		StringBuilder sb = new StringBuilder();
		ArrayList<String> strings = new ArrayList<>();
		// Read File Line By Line
		while ((strLine = br.readLine()) != null) {
			sb.append(strLine);
			System.out.println("Ligne = " + strLine);
			if (strLine.equals("")) {
				strLine = sb.toString();
				strings.add(strLine);
				sb = new StringBuilder();
			}
		}
		strLine = sb.toString();
		strings.add(strLine);
		br.close();

		// create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		objectMapper.setVisibilityChecker(VisibilityChecker.Std
				.defaultInstance().withFieldVisibility(
						JsonAutoDetect.Visibility.ANY));
		ArrayList<JsonData> jDatas = new ArrayList<JsonData>();
		// convert json string to object
		for (String s : strings) {
			jDatas.add(objectMapper.readValue(s, JsonData.class));
		}
		jDatas.stream().forEach(jd -> System.out.println(jd + "\n"));
		return jDatas.get(0);
	}
	
	public String giveJobByPriority(){
		
		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		sb.append("\"JobId\": \""+JobId+"\",\n");
		sb.append("\"WorkerVersion\": \""+WorkerVersionNumber+"\",\n");
		sb.append("\"WorkerURL\": \""+WorkerURL+"\",\n");
		sb.append("\"WorkerClassName\": \""+WorkerClassName+"\",\n");
		sb.append("\"Task\": \""+JobTaskNumber+"\",\n");
		return sb.toString();
	}

	@Override
	public String toString() {
		return "JobId : " + JobId + "\nJobTaskNumber : " + JobTaskNumber
				+ "\nJobDescription : " + JobDescription + "\nJobPriority : "
				+ JobPriority + "\nWorkerVersionNumber : "
				+ WorkerVersionNumber + "\nWorkerURL : " + WorkerURL
				+ "\nWorkerClassName : " + WorkerClassName;
	}

}
