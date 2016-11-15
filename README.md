# Arithmetic Word Problem Solver


### Publication

Code and data used for the paper

Subhro Roy and Dan Roth.
Unit Dependency Graph and its Application to Arithmetic Word Problem Solving
AAAI 2017

Also includes a cleaner implementation of our solver described in

Subhro Roy and Dan Roth
Solving General Arithmetic Word Problems
EMNLP 2015

If you use the code or data, please cite the above publications.


### Data

Data can be found in the folder data/.


### Instructions to run the code

1. sh run.sh Rel : Trains and tests the relevance classifier. Output file log/Rel.out

2. sh run.sh Pair : Trains and tests the LCA classifier. Output file log/Pair.out

3. sh run.sh Vertex : Trains and tests the vertex label classifier. Output file log/Vertex.out

4. sh run.sh Edge : Trains and tests the edge label classifier. Output file log/Edge.out

5. sh run.sh LCA : Trains and tests the LCA++ solver (EMNLP 2015)

6. sh run.sh UnitDep : Trains and tests the UnitDep solver (AAAI 2017)


### Other issues
 
Please send any suggestions, comments, issues to sroy9@illinois.edu.





