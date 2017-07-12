package demo;

import java.io.File;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;

public class Server {
	
	public static String logFile = "/shared/trollope/sroy9/arithmetic_demo/log.txt";
	
	public String rain(String input) throws Exception {
		try {
			if(input.trim().equals("")) return "";
			FileUtils.writeStringToFile(new File(logFile),
					"\n"+new Date()+" : Query : "+input+"\n", true);
			String answer = Demo.answerQuestion(input);
			FileUtils.writeStringToFile(new File(logFile),
					new Date()+" : Returned : "+answer+"\n\n", true);
			return answer;
		} catch (Exception e) {
			e.printStackTrace();
			return "Sorry, we could not solve it";
		}
	}

	public static void startServer(int portNumber) {
		try {
			System.out.println("Attempting to start XML-RPC Server...");
			WebServer webServer = new WebServer(portNumber);
			XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();
			PropertyHandlerMapping phm = new PropertyHandlerMapping();
			phm.addHandler("sample", Server.class); //new JavaServer().getClass());
			xmlRpcServer.setHandlerMapping(phm);
			XmlRpcServerConfigImpl serverConfig = (XmlRpcServerConfigImpl) xmlRpcServer.getConfig();
			serverConfig.setEnabledForExtensions(true);
			serverConfig.setContentLengthOptional(false);
			webServer.start();
			System.out.println("Started successfully.");
			System.out.println("Accepting requests. (Halt program to stop.)");
		} catch (Exception exception) {
			System.err.println("JavaServer: " + exception);
		}
	}
}