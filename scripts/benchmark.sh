#!/bin/bash

# Sample script to be used to run the project on a benchmark.
# Set the paths according to your installation. All paths must be full paths.
# Arguments: 1) name of benchmark

# Installed path of Java 8 JDK
java_install_path=

# Path to the directory containing all benchmarks. The subdirectories here must
# contain individual benchmarks 
benchmarks_base_path=

# The soot jar to be used.
soot_path=`realpath ../soot/sootclasses-trunk-jar-with-dependencies.jar`

# Path to stava repository
stava_path=`realpath ..`

# The directory inside which stava will output the results.
output_base_path=

java_compiler="${java_install_path}/bin/javac"
java_vm="${java_install_path}/bin/java"

if [[ $1 == "dacapo" ]]; then
	benchmark_path="${benchmarks_base_path}/dacapo"
	output_path="${output_base_path}/dacapo"
	main_class="Harness"
else
	echo path not recognised
	exit 0
fi

find  ${stava_path}/src -type f -name '*.class' -delete
echo compiling...
$java_compiler -cp $soot_path:${stava_path}/src ${stava_path}/src/main/Main.java
echo compiled!
echo launching...
$java_vm -classpath $soot_path:${stava_path}/src main.Main $java_install_path true $benchmark_path $main_class $output_path
