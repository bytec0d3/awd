#!/usr/bin/env bash

#-----------------------------------------------------------------------------------------------------------------------
# MAIN
#-----------------------------------------------------------------------------------------------------------------------



nodes="settings_paper/baseline_nodes.txt"
scenario="settings_paper/scenarios/fixed_office.txt"


DECISION_TIMES=(30 60 90 120)

for t in "${DECISION_TIMES[@]}"; do

    echo "AutonomousHost.decisionTimeS = $t" > parameters_${t}.config
    echo "Scenario.name = big_office_$t" >> parameters_${t}.config

    ./run_bin.sh -n ${nodes} -s ${scenario} -p parameters_${t}.config &

done
