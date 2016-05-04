rm *.class
cp ../Agentes/build/classes/*.class .
export CLASSPATH=".:../jade/lib/jade.jar"
echo $CLASSPATH