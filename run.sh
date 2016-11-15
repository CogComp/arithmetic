if [ "$#" -eq 0 ]
then echo "Usage : sh run.sh <dataset_dir> <module>(optional)(Rel/Pair/Tune)"
    exit 2
fi

if [ "$2" = "Rel" ]
then
	echo "Running RelDriver"
	java -cp target/classes/:target/dependency/* relevance.RelDriver crossVal true $1 1>log/Rel.out
fi
if [ "$2" == "Pair" ] 
then
	echo "Running PairDriver"
	java -cp target/classes/:target/dependency/* pair.PairDriver crossVal true $1 1>log/Pair.out
fi    
if [ "$2" == "Run" ] 
then
	echo "Running RunDriver"
	java -cp target/classes/:target/dependency/* run.RunDriver crossVal true $1 1>log/Run.out
fi    
if [ "$2" == "Rate" ] 
then
	echo "Running RateDriver"
	java -cp target/classes/:target/dependency/* rate.RateDriver crossVal true $1 1>log/Rate.out
fi    
if [ "$2" == "All" ]
then
	echo "Running Tune and Retrain"
	java -cp target/classes/:target/dependency/* constraints.ConsDriver tunedCrossValAndRetrain $1 1>log/Inf.out
fi
if [ "$2" == "GraphDecompose" ] 
then
	echo "Running GraphDecompose"
	java -cp target/classes/:target/dependency/* constraints.GraphDriver tunedCrossValAndRetrain $1 1>log/GraphDecompose.out
fi    
if [ "$2" == "GraphJoint" ] 
then
	echo "Running GraphJoint"
	java -cp target/classes/:target/dependency/* graph.GraphDriver crossVal true $1 1>log/GraphJoint.out
fi    

