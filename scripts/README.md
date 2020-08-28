# Scripts

These sample scripts are can be used for running stava. The paths need to be set in each file before they can be used. Each script requires a java installation path to be set. It is also required to specify a directory into which stava will output the results including the jimple files for all bodies. This may be disabled in the [sootArgs](https://github.com/42niks/stava/blob/master/src/main/GetSootArgs.java).

## benchmark.sh
It is expected that benchmarks are all stored in a directory which is to be set as `benchmarks_base_path`. For example, for dacapo, the main class is `Harness`. This has been set in the sample script.

## run.sh
Path to the test application has to be set as `test_path`. Since the bytecode supported by soot has to be java 8, the compilation commands have been inserted. Optionally, this may be disabled.