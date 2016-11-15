package demo;

import java.io.File;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;

import constraints.ConsInfSolver;
import structure.Node;
import structure.Problem;
import structure.QuantSpan;
import utils.Params;
import utils.Tools;

public class Server {
	
	public static String logFile = "/shared/trollope/sroy9/arithmetic_demo/log.txt";
	
	public String rain(String input) throws Exception {
		try {
			if(input.trim().equals("")) return "";
			FileUtils.writeStringToFile(new File(logFile), 
					"\n"+new Date()+" : Query : "+input+"\n", true);
			Problem problem = new Problem(0, input.trim(), 0);
			problem.quantities = Tools.quantifier.getSpans(input.trim());
			String newQues = "";
			int index = 0;
			for(QuantSpan qs : problem.quantities) {
				newQues += problem.question.substring(index, qs.start);
				newQues += qs.val;
				index = qs.end;
			}
			newQues += problem.question.substring(index);
			problem = new Problem(0, newQues, 0);
			problem.quantities = Tools.quantifier.getSpans(problem.question);
			System.out.println(problem.question);
			System.out.println(Arrays.asList(problem.quantities));
			// First try CFG parser
			String cfgParseResult = Derivation.solveByDerivation(problem);
			if(cfgParseResult != null && !cfgParseResult.trim().equals("")) {
				FileUtils.writeStringToFile(new File(logFile), 
						new Date()+" : Returned : "+cfgParseResult+"\n", true);
				return Trainer.genNumberQueryHtml(cfgParseResult);
			}
			// Next arithmetic solver
			problem.extractAnnotations();
			Node node = ConsInfSolver.constrainedInf(problem, Trainer.relModel, 
					Trainer.pairModel, Trainer.runModel, Trainer.rateModel);
			FileUtils.writeStringToFile(new File(logFile), 
					new Date()+" : Returned : "+node.toString()+" = "+node.getValue()+"\n\n", true);
			return Trainer.genTableHtml(problem, node.toString()+" = "+node.getValue());
		} catch (Exception e) {
			e.printStackTrace();
			return Trainer.genErrorHtml("Sorry, we could not solve it");
		}
	}

	public static void main(String[] args) throws Exception {
		Trainer.tuneAndRetrain(Params.allArithDir);
		Trainer.loadModels();
		startServer(8182);
//		String input = "";
//		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//		while(true) {
//			System.out.println("Enter problem : ");
//			input = br.readLine();
//			if(input.trim().equalsIgnoreCase("n")) break;
//			System.out.println(new Server().rain(input));
//		}
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
//			serverConfig.setKeepAliveEnabled(true);
//			boolean res = serverConfig.isKeepAliveEnabled();
			webServer.start();
			System.out.println("Started successfully.");
			System.out.println("Accepting requests. (Halt program to stop.)");
		} catch (Exception exception) {
			System.err.println("JavaServer: " + exception);
		}
	}
}