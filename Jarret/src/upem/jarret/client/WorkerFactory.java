package upem.jarret.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import upem.jarret.worker.Worker;

public class WorkerFactory {

	/**
	 *
	 * @param url
	 *            location of the jar
	 * @param className
	 *            name of the class implementing upem.jarret.worker.Worker
	 * @return An instance of className from the jar located at URL
	 * @throws java.net.MalformedURLException
	 *             if the url is malformed
	 * @throws java.lang.ClassNotFoundException
	 *             if the class className was not found in the jar
	 * @throws java.lang.IllegalAccessException
	 * @throws java.lang.InstantiationException
	 *             if the creation of the instance failed
	 * @throws java.lang.ClassCastException
	 *             (unchecked) if the cast to upem.jarret.worker.Worker failed
	 */
	public static Worker getWorker(String url, String className)
			throws MalformedURLException, ClassNotFoundException,
			IllegalAccessException, InstantiationException {
		URL urls[] = { new URL("jar", "", url + "!/") };
		System.out.println("test1");
		URLClassLoader urlClassLoader = new URLClassLoader(urls, Thread
				.currentThread().getContextClassLoader());
		System.out.println("test2");
		Class<? extends Object> theClass = Class.forName(className, true,
				urlClassLoader);
		System.out.println("test3");
		theClass.newInstance();
		return (upem.jarret.worker.Worker) theClass.newInstance();

	}
}