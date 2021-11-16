#!/bin/bash
# java_install_path="/usr/lib/jvm/java-8-oracle/"
java_install_path="/usr/lib/jvm/default/"
# java_install_path="/wd/users/t17041/benchmarks/jdk-11.0.8/"
# Path to the directory containing all benchmarks. The subdirectories here must
# contain individual benchmarks 
benchmarks_base_path=`realpath ../../benchmarks/`
# The soot jar to be used.
soot_path=`realpath ../soot/sootclasses-trunk-jar-with-dependencies.jar`
# soot_path="/home/dj/github/soot/target/sootclasses-trunk-jar-with-dependencies.jar"
# Path to stava repository
wd=`pwd`
stava_path=`realpath ..`
stava_run="${stava_path}/src/"
# The directory inside which stava will output the results.
output_base_path=`realpath ../../out/`
java_compiler="${java_install_path}/bin/javac"
java_vm="${java_install_path}/bin/java"

benchmark_path="${benchmarks_base_path}/dacapo"
output_path="${output_base_path}/dacapo/$1"
main_class="Harness"

find  ${stava_path}/src -type f -name '*.class' -delete
echo compiling...
$java_compiler -cp $soot_path:${stava_path}/src ${stava_path}/src/main/Main.java
echo compiled!

cd $benchmark_path

rm -rf out scratch

$java_vm -javaagent:poa-trunk.jar -jar dacapo-9.12-MR1-bach.jar $1 -s small

cd $wd

execute () {
    echo launching...
    echo $1 $2 $3
    $java_vm -Xmx50g -classpath $soot_path:$stava_run main.Main $java_install_path true $1 $2 $3 $4 $5
}

clean () {
    echo clearing output_files...
    find $1 -type f -name '*.res' -delete
    find $1 -type f -name '*.info' -delete    
    find $1 -type f -name 'stats.txt' -delete
}

clean $output_path

execute $benchmark_path $main_class $output_path $2 $3

cp $output_path/dacapo.res $output_base_path/res_files/$1_$4.res

cp $output_path/stats.txt $output_base_path/stat_files/stats_$1_$4.txt
