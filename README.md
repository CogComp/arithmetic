# Arithmetic Word Problem Solver


### Publication

Code and data used for the paper

  Subhro Roy and Dan Roth.  
  Unit Dependency Graph and its Application to Arithmetic Word Problem Solving.  
  AAAI 2017.

Also includes a cleaner implementation of our solver described in

  Subhro Roy and Dan Roth.  
  Solving General Arithmetic Word Problems.  
  EMNLP 2015.

If you use the code or data, please cite the above publications.


### Data

Data can be found in the folders data/allArith, data/allArithLex, and data/allArithTmpl. Each data folder contains the following :

* questions.json :  file with all questions and corresponding answers
* rateAnnotations.txt : file containing annotations for vertex labels of UDG. Each line contains a question id, and indices of the quantities which are rates. The index -1 refers to the question part, indicating that the question is asking for a rate.
* fold\<i\>.txt : where i=0, ..., \<num_folds\> - 1. Each fold file contains indices of the problems belonging to the fold. Each problem from questions.json should be present in one of the fold files.   


### Instructions to run the code

You will need to have maven installed in your system. To download the 
dependencies, run

    mvn dependency:copy-dependencies
        
Next compile using : 
    
    mvn compile     

Finally, run the following:

  sh run.sh \<dataset_folder\> \<mode\>

where 
  
  * dataset_folder : folder with questions.json, rateAnnotations.txt, and fold files, as described above.
  * mode : takes one of the following options
    * Rel : Trains and tests the relevance classifier. Output file log/Rel.out
    * Pair : Trains and tests the LCA classifier. Output file log/Pair.out
    * Vertex : Trains and tests the vertex label classifier. Output file log/Vertex.out
    * Edge : Trains and tests the edge label classifier. Output file log/Edge.out
    * GraphDecompose : Trains and tests the decomposed UDG prediction model. Output file log/GraphDecompose.out
    * GraphJoint : Trains and tests the joint UDG prediction model. Output file log/GraphJoint.out
    * LCA : Trains and tests the LCA++ solver (EMNLP 2015). Output file log/LCA.out
    * UnitDep : Trains and tests the UnitDep solver (AAAI 2017). Output file log/UnitDep.out


### Other issues
 
Please send any suggestions, comments, issues to sroy9@illinois.edu.





