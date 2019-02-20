docker run -it --network akka-network --mount type=bind,source=/Users/plasne/Documents/akka-quickstart-java/,target=/akka -e "PORT=2551" --name node0 java

docker run -it --network akka-network --mount type=bind,source=/Users/plasne/Documents/akka-quickstart-java/,target=/akka --name node1 -e "PORT=2551" java

cd akka/
mvn compile exec:exec

node receiver.js --port 8100
