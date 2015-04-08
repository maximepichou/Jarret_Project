package upem.jarret.worker;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class ClientTask {
	private long jobId;
	private String workerVersion;
	private String workerURL;
	private String workerClassName;
	private int task;
	private String result;
	private int secondsToSleep;

	public ClientTask(long jobId, String workerVersion, String workerURL,
			String workerClassName, int task) {
		if (jobId < 0) {
			throw new IllegalArgumentException("JobId is not valid");
		}
		if (task < 0) {
			throw new IllegalArgumentException("task is not valid");
		}
		this.jobId = jobId;
		this.workerVersion = Objects.requireNonNull(workerVersion);
		this.workerURL = Objects.requireNonNull(workerURL);
		this.workerClassName = Objects.requireNonNull(workerClassName);
		this.task = task;
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
		JsonFactory jfactory = new JsonFactory();
		long jobId = -1;
		String workerVersion = "";
		String workerURL = "";
		String workerClassName = "";
		int task = -1;
		try {
			JsonParser jParser = jfactory.createParser(content);
			// loop until token equal to "}"
			while (jParser.nextToken() != JsonToken.END_OBJECT) {

				String fieldname = jParser.getCurrentName();
				switch (fieldname) {
				case "JobId":
					jParser.nextToken();
					jobId = Long.valueOf(jParser.getText());
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
					task = jParser.getIntValue();
					break;
				case "ComeBackInSeconds":
					return new ClientTask(jParser.getIntValue());

				}
			}
			jParser.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		if (jobId == -1 || task == -1 || workerVersion.equals("")
				|| workerURL.equals("") || workerURL.equals(""))
			throw new IllegalStateException();
		return new ClientTask(jobId, workerVersion, workerURL, workerClassName,
				task);
	}

	public void doWork() throws InvocationTargetException,
			MalformedURLException, ClassNotFoundException,
			IllegalAccessException, InstantiationException {
		Worker worker = WorkerFactory.getWorker(workerURL, workerClassName);
		result = worker.compute(task);
	}
	
	public void checkResult(){
		JsonFactory jfactory = new JsonFactory();
		try {
			JsonParser jParser = jfactory.createParser(result);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
