#!/usr/bin/env bash

#-----------------------------------------------------------------------------------------------------------------------
# HELP function
#-----------------------------------------------------------------------------------------------------------------------
function help {

usage="$(basename "$0") [-h] -s <file> -m <file> -p <file> -- runs the Autonomous Wi-Fi Direct (AWD) simulations.

where:
    -h      show this help
    -s      scenario config file
    -m      max clients config file
    -p      parameters config file"

printf "${usage}\n"
exit -1

}

#-----------------------------------------------------------------------------------------------------------------------
# Check the script's arguments
#-----------------------------------------------------------------------------------------------------------------------
function check_arguments {

if [ "$#" == 1 ] && [ "$1" = "-h" ]; then
    help
elif [ "$#" -ne 6 ]; then
    echo "Error: Illegal number of arguments"
    help
fi

while [[ $# -gt 1 ]]
do
key="$1"

case $key in
    -s)
    SCENARIO_CONFIG="$2"
    shift # past argument
    ;;
    -m)
    MAX_CLIENTS_CONFIG="$2"
    shift # past argument
    ;;
    -p)
    PARAMETERS_CONFIG="$2"
    shift # past argument
    ;;
esac
shift # past argument or value
done

if [ -z "$SCENARIO_CONFIG" ] || [ -z "$PARAMETERS_CONFIG" ] || [ -z "$MAX_CLIENTS_CONFIG" ]; then
    echo "Error: Illegal arguments"
    help
fi

}

#-----------------------------------------------------------------------------------------------------------------------
# MAIN
#-----------------------------------------------------------------------------------------------------------------------
check_arguments "$@"

jar_path="awd-sim.jar"

java -jar ${jar_path} -b 1 ${SCENARIO_CONFIG} ${PARAMETERS_CONFIG} ${MAX_CLIENTS_CONFIG}
