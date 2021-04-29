#!/bin/bash
# Sample script to be used to run the project on a benchmark.
# Set the paths according to your installation. All paths must be full paths.
# Arguments: 1) name of benchmark
# Installed path of Java 8 JDK
java_install_path="/usr/lib/jvm/java-8-oracle/"
# java_install_path=""
# java_install_path="/wd/users/t17041/benchmarks/jdk-11.0.8/"
# Path to the directory containing all benchmarks. The subdirectories here must
# contain individual benchmarks 
benchmarks_base_path=`realpath ../../benchmarks/`
# The soot jar to be used.
soot_path=`realpath ../soot/sootclasses-trunk-jar-with-dependencies.jar`
# soot_path="/home/dj/github/soot/target/sootclasses-trunk-jar-with-dependencies.jar"
# Path to stava repository
stava_path=`realpath ..`
stava_run="${stava_path}/src/"
# The directory inside which stava will output the results.
output_base_path=`realpath ../out/`
java_compiler="${java_install_path}/bin/javac"
java_vm="${java_install_path}/bin/java"

find  ${stava_path}/src -type f -name '*.class' -delete
echo compiling...
$java_compiler -cp $soot_path:${stava_path}/src ${stava_path}/src/main/Main.java
echo compiled!

execute () {
    echo launching...
    echo $1 $2 $3
    $java_vm -Xmx1g -classpath $soot_path:$stava_run main.Main $java_install_path true $1 $2 $3
}
clean () {
    echo clearing output_files...
    find $1 -type f -name '*.res' -delete
    find $1 -type f -name '*.info' -delete    
    find $1 -type f -name 'stats.txt' -delete
}
if [[ $1 == "dacapo" ]]; then
    benchmark_path="${benchmarks_base_path}/dacapo"
    output_path="${output_base_path}/dacapo"
    main_class="Harness"
elif [[ $1 == "jbb" ]]; then
    benchmark_path="${benchmarks_base_path}/jbb/"
    output_path="${output_base_path}/jbb/"
    main_class="spec.jbb.JBBmain"
elif [[ $1 == "jgfall" ]]; then
    for dir in ${benchmarks_base_path}/jgf/JGF*
    do
        lib=${dir##*/}
        echo $lib
        output_path="${output_base_path}/jgf/${lib}"
        clean $output_path
        execute $dir $lib $output_path
    done
    exit 0
elif [[ $1 == "barrier" ]]; then
    benchmark_path="${benchmarks_base_path}/jgf/JGFBarrierBench"
    output_path="${output_base_path}/jgf/JGFBarrierBench"
    main_class="JGFBarrierBench"
elif [[ $1 == "montecarlo" ]]; then
    benchmark_path="${benchmarks_base_path}/jgf/JGFMonteCarloBenchSizeA"
    output_path="${output_base_path}/jgf/JGFMonteCarloBenchSizeA"
    main_class="JGFMonteCarloBenchSizeA"
elif [[ $1 == "raytracer" ]]; then
    benchmark_path="${benchmarks_base_path}/jgf/JGFRayTracerBenchSizeA"
    output_path="${output_base_path}/jgf/JGFRayTracerBenchSizeA"
    main_class="JGFRayTracerBenchSizeA"
elif [[ $1 == "crypt" ]]; then
    benchmark_path="${benchmarks_base_path}/jgf/JGFCryptBenchSizeA"
    output_path="${output_base_path}/jgf/JGFCryptBenchSizeA"
    main_class="JGFCryptBenchSizeA"
else
    echo path not recognised
    exit 0
fi
clean $output_path
find  ${stava_path}/src -type f -name '*.class' -delete
echo compiling...
$java_compiler -cp $soot_path:${stava_path}/src ${stava_path}/src/main/Main.java
echo compiled!
echo launching...
execute $benchmark_path $main_class $output_path
