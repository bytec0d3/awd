#!/usr/bin/env bash

#-----------------------------------------------------------------------------------------------------------------------
# MAIN
#-----------------------------------------------------------------------------------------------------------------------
nodes_positions=$1
scenario=$2
max_clients_file=$3
base_name=$4

DECISION_TIMES=(30 60 90 120)
TRANSMIT_RANGES=(30 40 50 60 80 90 100)

for tr in "${TRANSMIT_RANGES[@]}"; do
  for t in "${DECISION_TIMES[@]}"; do

    printf "
    Group1.movementModel = ExternalMovement
    ExternalMovement.file = ${nodes_positions}

    AutonomousHost.decisionTime = ${t}
    AutonomousHost.travellingProb = 0.6
    AutonomousHost.destroyGroupAfterPercRes = 0.2

    AutonomousHost.utilityResourcesWeight = 0.25
    AutonomousHost.utilityNearbyNodesWeight = 0.25
    AutonomousHost.utilityCapacityNearbyWeight = 0.25
    AutonomousHost.utilityStabilityWeight = 0.25

    AutonomousHost.stabilityWindowSize = 300
    AutonomousHost.prevStabilityWeight = 0.4
    AutonomousHost.currentStabilityWeight = 0.6

    firstinterface.transmitRange = ${tr}
    firstinterface.scanInterval = 0
    firstinterface.maxDelayFirstScan = 5
    firstinterface.blacklistTime = 300
    Scenario.name = "${base_name}"_dt_"${t}"_tr"${tr} > parameters_dt_${t}_tr_${tr}.conf

    ./run_bin.sh -s ${scenario} -p parameters_dt_${t}_tr_${tr}.conf -m ${max_clients_file} &
  done
done