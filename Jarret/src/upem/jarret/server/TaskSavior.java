package upem.jarret.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

import upem.jarret.client.ClientTask;

public class TaskSavior {

	private final String pathJobs;

	public TaskSavior(String pathJobs) {
		this.pathJobs = Objects.requireNonNull(pathJobs);
	}

	public boolean saveJob(String job) throws IOException {
		ClientTask cTask = ClientTask.create(job);
		if (!cTask.isValidJSON(job)) {
			return false;
		}
		String jobId = cTask.getJobId();
		String fileName = pathJobs + jobId + ".job";
		File f = new File(fileName);
		f.getParentFile().mkdirs();

		StringBuilder sb = new StringBuilder();
		sb.append("Task : ").append(cTask.getTask()).append("\n");
		sb.append("ClientId : ").append(cTask.getClientId()).append("\n");
		boolean containsError = !cTask.getError().equals("");
		sb.append(containsError ? "Error : " : "Answer : ")
				.append(containsError ? cTask.getError() : cTask.getAnswer())
				.append("\n");
		sb.append("\n");
		byte[] byteArray = sb.toString().getBytes();

		if (f.exists() && !f.isDirectory()) {
			try (FileOutputStream fos = new FileOutputStream(f, true)) {
				fos.write(byteArray);
			}
		} else {
			try (FileOutputStream fos = new FileOutputStream(f, false)) {
				StringBuilder header = new StringBuilder();
				header.append("JobId : ").append(cTask.getJobId()).append("\n");
				header.append("WorkerVersion : ")
						.append(cTask.getWorkerVersion()).append("\n");
				header.append("WorkerURL : ").append(cTask.getWorkerURL())
						.append("\n");
				header.append("WorkerClassName : ")
						.append(cTask.getWorkerClassName()).append("\n");
				header.append("\n");
				fos.write(header.toString().getBytes());
				fos.write(byteArray);
			}
		}
		System.out.println("Task " + cTask.getTask() + " saved on file " + fileName );
		return true;
	}

}
