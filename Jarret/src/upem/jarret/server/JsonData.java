package upem.jarret.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonData {
	private int JobId;
	private int JobTaskNumber;
	private String JobDescription;
	private int JobPriority;
	private int WorkerVersionNumber;
	private String WorkerURL;
	private String WorkerClassName;
	
	public static JsonData create() throws JsonParseException, IOException{
		JsonFactory jfactory = new JsonFactory();
		
		 //read json file data to String
		ArrayList<Integer> integers = new ArrayList<>();
       
        /*List<String> strings = Files.readAllLines(Paths.get("JarRetJobs.json"));
        for(int i = 0; i < strings.size(); i++){
        	if(strings.get(i).equals("}")){
        		integers.add(i);
        	}
        }*/
        
        //create ObjectMapper instance
        
        Scanner sc = null;
        ArrayList<StringBuilder> strings = new ArrayList<>();
        int i = 0;
            try {
            	sc = new Scanner(new File("JarRetJobs.json"));
                while (sc.hasNextLine()) {
                    for (char c : sc.next().toCharArray()) {
                    	strings.add(new StringBuilder());
                        if(c == '}'){
                        	i++;
                        } 
                    }
                }
            } finally {
                if (sc != null)
                    sc.close();
            }
            strings.stream().forEach(s->System.out.println(s.toString()));
 
       
        //convert json string to object
        return null;
	}

}
