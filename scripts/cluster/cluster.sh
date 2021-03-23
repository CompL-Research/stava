#!/bin/bash

#PBS -q day

#PBS -N tomcat_jdk

#PBS -l walltime=12:30:00

echo "Running on: "

cat ${PBS_NODEFILE}

echo "Program Output Begins: "

cd ${PBS_O_WORKDIR}/../

# ./benchmark.sh dacapo

./run.sh
# ./jdk.sh

# ./benchmark.sh dacapo

