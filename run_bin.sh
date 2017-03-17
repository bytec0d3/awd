#!/usr/bin/env bash

#-----------------------------------------------------------------------------------------------------------------------
# HELP function
#-----------------------------------------------------------------------------------------------------------------------
function help {

usage="$(basename "$0") [-h] -n <file> -s <file> -p <file> -- runs the Autonomous Wi-Fi Direct (AWD) simulations.

where:
    -h      show this help
    -n      nodes config file
    -s      scenario config file
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
    -n)
    NODES_CONFIG="$2"
    shift # past argument
    ;;
    -s)
    SCENARIO_CONFIG="$2"
    shift # past argument
    ;;
    -p)
    PARAMETERS_CONFIG="$2"
    shift # past argument
    ;;
esac
shift # past argument or value
done

if [ -z "$NODES_CONFIG" ] || [ -z "$SCENARIO_CONFIG" ] || [ -z "$PARAMETERS_CONFIG" ]; then
    echo "Error: Illegal arguments"
    help
fi

}


#-----------------------------------------------------------------------------------------------------------------------
# MAIN
#-----------------------------------------------------------------------------------------------------------------------
check_arguments "$@"

java -jar awd-sim.jar -b 1 ${SCENARIO_CONFIG} ${NODES_CONFIG} ${PARAMETERS_CONFIG}
