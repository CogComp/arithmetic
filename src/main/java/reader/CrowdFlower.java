package reader;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class CFJudgmentData {
    public String question1;
}

class CFJudgment {
    public int worker_id;
    public boolean tainted;
    public CFJudgmentData data;
}

class CFResults {
    public List<CFJudgment> judgments;
}

class CFData {
    public String question;
    public String answer;
}

public class CrowdFlower {
    public int id;
    public CFData data;
    public CFResults results;

    public static List<CrowdFlower> readCrowdFlowerFile(String fileName)
            throws IOException {
        List<CrowdFlower> problems = new ArrayList<>();
        for(String jsonLine : FileUtils.readLines(new File(fileName))) {
            CrowdFlower prob =new Gson().fromJson(jsonLine,
                    new TypeToken<CrowdFlower>(){}.getType());
            problems.add(prob);
        }
        return problems;
    }

    public static void main(String args[]) throws Exception {
        List<CrowdFlower> probs = readCrowdFlowerFile("data/job_1012604.json");
        for(CrowdFlower prob : probs) {
            String inp = prob.data.question;
            for(CFJudgment judgment : prob.results.judgments) {
                if(judgment.tainted) continue;
                String mod = judgment.data.question1;
                if(inp.trim().equals(mod.trim())) {
                    System.out.println(prob.id+" : Same Problem");
                }
            }
        }
    }

}


