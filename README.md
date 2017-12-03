# Arithmetic Word Problem Solver

This repository contains the source code and data for automatic math
word problem solving. It contains the implementation of 3 distinct
arithmetic word problem solvers. This has been developed by Subhro Roy
at the Cognitive Computation Group at the University of Illinois,
Urbana Champaign.
 
### Publication

This repository has been used in the following publications. Please
cite them if you use the code or data.

~~~~
  Subhro Roy and Dan Roth.  
  Mapping to Declarative Knowledge for Word Problem Solving.  
  In Preparation.
~~~~
~~~~
  Subhro Roy and Dan Roth.  
  Unit Dependency Graph and its Application to Arithmetic Word Problem Solving.  
  AAAI 2017.
~~~~
~~~~
  Subhro Roy and Dan Roth.  
  Solving General Arithmetic Word Problems.  
  EMNLP 2015.
~~~~



### Data

Data can be found in the file data/questions.json. This file
constitutes our largest dataset Aggregate. All other released
datasets, namely allArith, allArithLex, allArithTmpl, aggregateLex,
and aggregateTmpl, are subsets of Aggregate. 

The data file has a json format. Each datapoint is represented as
~~~~
[
 {
    "iIndex": 1232857298,
    "sQuestion": "A large bag of balls was kept under Haley’s bed. Her mom placed the balls in bags for children in foster homes. If every bag can contain 4.0 balls, and Haley has 36.0 bags, how many balls total will be donated?",
    "quants": [
      4.0,
      36.0
    ],
    "lAlignments": [
      1,
      0
    ],
    "lEquations": [
      "X=(36.0 * 4.0)"
    ],
    "lSolutions": [
      144.0
    ],
    "rates": [0]
  },
...
]
~~~~

"iIndex" is a unique integer identifier, "sQuestion" is the problem
text, quants is the ordered list of numbers detected from the text,
"lAlignments" refer to the alignment of numbers from the text to the
equation (if i-th position has j, then the i-th number in the equation
is mapped to the j-th number from the quants), "lEquations" is the
list of equations, and lSolutions is a list of solutions. "rates"
indicate the indices of quantities which represent a rate
relationship. If the question represents a rate, "rate" will have -1.

The data/ directory has several folders, each representing a dataset.
A dataset directory usually has a few fold files, each fold file
contains indices of the problems belonging to the fold. The dataset
comprises the union of all the folds present in the folder.


### Instructions to run the code

You will need to have maven installed in your system. To download the 
dependencies, run

    mvn dependency:copy-dependencies
        
Next compile using : 
    
    mvn compile     

The main interface to the code is the script run.sh. It has the 
following arguments:

* --data <data_file> : Data file containing all the questions (uses data/questions.json if not provided)  
* --mode < mode > : Mode takes one of the following options
    * Rel : Trains and tests the relevance classifier. Output file log/Rel.out
    * Pair : Trains and tests the LCA classifier. Output file log/Pair.out
    * Vertex : Trains and tests the vertex label classifier. Output file log/Vertex.out
    * Edge : Trains and tests the edge label classifier. Output file log/Edge.out
    * GraphDecompose : Trains and tests the decomposed UDG prediction model. Output file log/GraphDecompose.out
    * GraphJoint : Trains and tests the joint UDG prediction model. Output file log/GraphJoint.out
    * LCA : Trains and tests the LCA++ solver (EMNLP 2015). Output file log/LCA.out
    * UnitDep : Trains and tests the UnitDep solver (AAAI 2017). Output file log/UnitDep.out
    * Coref : Trains and tests the coreference model or the rule selection model. Output file log/Coref.out
    * Logic : Trains and tests the logic model with gold rule selection and relevance. Output file log/Logic.out
    * E2ELogic : Trains and tests the end to end logic system. Output file log/E2ELogic.out
* --train <train_fold_1> <train_fold_2> ... <train_fold_n> : List of fold files for training.
* --test <test_fold_1> <test_fold_2> ... <test_fold_n> : List of fold files for testing.
* --cv <cv_fold_1> <cv_fold_2> ... <cv_fold_n> : List of fold files for n-fold cross validation. Will not 
  work when train and test are provided.
* --model_dir < dir > : Location to save model files (uses models/ if not provided).
* --print_mistakes : Option to print test questions the system got wrong.
* --print_correct : Option to print test questions the system got correct.
* --demo : Option to run terminal based demo of the system.
* --demo_server : Starts the web demo, only for Cogcomp users.  

Have a look at some of the scripts under scripts/ for examples of
commands used to generates results in the papers.  

### Other issues
 
Please send any suggestions, comments, issues to subhro@csail.mit.edu .





