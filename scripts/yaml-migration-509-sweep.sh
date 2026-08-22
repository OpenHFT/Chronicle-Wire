#!/usr/bin/env bash

set -eu

repository_root=$(cd "$(dirname "$0")/.." && pwd)
sweep_tmp=$(mktemp -d "${TMPDIR:-/tmp}/chronicle-wire-yaml-509.XXXXXX")
trap 'rm -rf "$sweep_tmp"' EXIT

cd "$repository_root"

git rev-parse HEAD > "$sweep_tmp/source-sha.txt"
java -version > "$sweep_tmp/java-version.txt" 2>&1
mvn -version > "$sweep_tmp/maven-version.txt" 2>&1

set +e
mvn -o clean test \
    -Dwire.testAsYaml=true \
    -Dtest='!ApacheStructExploitTest' \
    -Dmaven.test.failure.ignore=true \
    > "$sweep_tmp/maven.log" 2>&1
maven_status=$?
set -e

results_dir="$repository_root/target/yaml-migration-509"
mkdir -p "$results_dir/xml"
cp "$sweep_tmp/source-sha.txt" "$results_dir/"
cp "$sweep_tmp/java-version.txt" "$results_dir/"
cp "$sweep_tmp/maven-version.txt" "$results_dir/"
cp "$sweep_tmp/maven.log" "$results_dir/"

if ls target/surefire-reports/TEST-*.xml >/dev/null 2>&1; then
    cp target/surefire-reports/TEST-*.xml "$results_dir/xml/"
fi

awk '
    /<testcase name="/ {
        method = $0
        sub(/^.*<testcase name="/, "", method)
        sub(/".*$/, "", method)

        class_name = $0
        sub(/^.*classname="/, "", class_name)
        sub(/".*$/, "", class_name)
    }
    /<failure([ >])/ {
        print class_name "#" method "\tfailure"
    }
    /<error([ >])/ {
        print class_name "#" method "\terror"
    }
' "$results_dir"/xml/TEST-*.xml | sort > "$results_dir/failing-methods.tsv"

awk '
    /<testsuite / {
        tests = failures = errors = skipped = $0
        sub(/^.* tests="/, "", tests); sub(/".*$/, "", tests)
        sub(/^.* failures="/, "", failures); sub(/".*$/, "", failures)
        sub(/^.* errors="/, "", errors); sub(/".*$/, "", errors)
        sub(/^.* skipped="/, "", skipped); sub(/".*$/, "", skipped)
        total_tests += tests
        total_failures += failures
        total_errors += errors
        total_skipped += skipped
        if (failures + errors > 0)
            affected_classes++
    }
    END {
        print "tests\tfailures\terrors\tskipped\taffected_classes"
        print total_tests "\t" total_failures "\t" total_errors "\t" total_skipped "\t" affected_classes
    }
' "$results_dir"/xml/TEST-*.xml > "$results_dir/totals.tsv"

if [ "${YAML_MIGRATION_RUN_CRASH_REPRO:-false}" = "true" ]; then
    set +e
    mvn -o test \
        -Dwire.testAsYaml=true \
        -Dtest=ApacheStructExploitTest \
        > "$results_dir/apache-struct-exploit.log" 2>&1
    crash_status=$?
    set -e
    printf '%s\n' "$crash_status" > "$results_dir/apache-struct-exploit.exit-code"
    find "$repository_root" -maxdepth 2 -type f -name 'hs_err_pid*.log' \
        -exec cp '{}' "$results_dir/" ';'
fi

printf '%s\n' "$maven_status" > "$results_dir/maven.exit-code"
printf 'YAML migration evidence written to %s\n' "$results_dir"
exit "$maven_status"
