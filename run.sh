if [ "$#" -eq 0 ]
then echo "Usage : sh run.sh <dataset_dir> <module>(optional)(Rel/Pair/Tune)"
    exit 2
fi
if [ "$2" = "Rel" ]
then
	echo "Running RelDriver"
	java -cp target/classes/:target/dependency/* relevance.RelDriver crossVal true $1 1>log/Rel.out
fi
if [ "$2" = "Pair" ] 
then
	echo "Running PairDriver"
	java -cp target/classes/:target/dependency/* pair.PairDriver crossVal true $1 1>log/Pair.out
fi    
if [ "$2" = "Edge" ] 
then
	echo "Running RunDriver"
	java -cp target/classes/:target/dependency/* run.RunDriver crossVal true $1 1>log/Edge.out
fi    
if [ "$2" = "Vertex" ] 
then
	echo "Running RateDriver"
	java -cp target/classes/:target/dependency/* rate.RateDriver crossVal true $1 1>log/Vertex.out
fi
if [ "$2" = "GraphDecompose" ] 
then
	echo "Running GraphDecompose"
	java -cp target/classes/:target/dependency/* constraints.GraphDriver tunedCrossValAndRetrain $1 1>log/GraphDecompose.out
fi    
if [ "$2" = "GraphJoint" ] 
then
	echo "Running GraphJoint"
	java -cp target/classes/:target/dependency/* graph.GraphDriver crossVal true $1 1>log/GraphJoint.out
fi    
if [ "$2" = "LCA" ]
then
	echo "Running LCA++ solver"
	java -cp target/classes/:target/dependency/* constraints.ConsDriver tunedCrossValAndRetrain $1 true 1>log/LCA.out
fi    
if [ "$2" = "UnitDep" ]
then
	echo "Running UnitDep solver"
	java -cp target/classes/:target/dependency/* constraints.ConsDriver tunedCrossValAndRetrain $1 false 1>log/UnitDep.out
fi    

