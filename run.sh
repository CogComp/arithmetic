mkdir -p models
mkdir -p log
java -cp target/classes/:target/dependency/* demo.Driver "$@"
