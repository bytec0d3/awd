#/bin/bash

bin="awd-sim.jar"
base_scenario="settings_paper"
scenario_name="randomwaypoint"
algorithm="baseline"
battery_log="reports/"$scenario_name"_Autonomous_BatteryReport.txt"
exploit_log="reports/"$scenario_name"_Autonomous_ExploitReport.txt"
base_results_dir="../analysis/results"

results_dir=$base_results_dir"/"$scenario_name"_"$algorithm

configs=( "25" "50" "100" )

mkdir $results_dir

for config in "${configs[@]}"; do
	config_name=$config"_baseline_rwp"
	java -jar $bin -b 1 $base_scenario"/"$config_name".txt"
	mv $battery_log $results_dir"/"$config_name"_battery.txt"
	mv $exploit_log $results_dir"/"$config_name"_graph.txt"
done
