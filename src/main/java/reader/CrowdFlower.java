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
        System.out.println("Number of problems: "+probs.size());
        System.out.println("Input Problem: "+probs.get(50).data.question);
        System.out.println("Input Answer: "+probs.get(50).data.answer);
        System.out.println("Perturbed Problem: "+
                probs.get(50).results.judgments.get(0).data.question1);
    }

}


