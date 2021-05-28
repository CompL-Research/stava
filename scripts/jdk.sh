#!/bin/bash

# Sample script to be used to run the project on non-benchmark code.
# Set the paths according to your installation. All paths must be full paths.
# Instructions: ./run.sh ClassName
# Installed path of Java 8 JDK
java_install_path="/opt/soft/share/jdk1.8.0_144/"

# The soot jar to be used.
soot_path=`realpath ../soot/sootclasses-trunk-jar-with-dependencies.jar`

# Path to stava repository
stava_path=`realpath ..`

# The directory to be analysed.
test_path=`realpath ../tests/test1/`

# The directory inside which stava will output the results.
output_path=`realpath ../out/jdk/`

java_compiler="${java_install_path}/bin/javac"
java_vm="${java_install_path}/bin/java"

# find $test_path -type f -name '*.class' -delete
# echo compiling test...
$java_compiler -cp $test_path ${test_path}/*.java
echo compiled!

find ${stava_path}/src -type f -name '*.class' -delete
find $output_path -type f -name '*.info' -delete
find $output_path -type f -name '*.res' -delete
find $output_path -type f -name 'stats.txt' -delete

echo compiling stava...
$java_compiler -cp $soot_path:${stava_path}/src ${stava_path}/src/main/Main.java
echo compiled!
echo launching stava...
$java_vm -Xmx80g -classpath $soot_path:${stava_path}/src main.Main $java_install_path false $test_path JDK $output_path
