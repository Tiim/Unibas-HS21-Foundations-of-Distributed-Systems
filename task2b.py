import os
import subprocess
import re
from time import sleep


# Delay
# sudo tc qdisc add dev lo root handle 1:0 netem delay 100msec
# sudo tc qdisc del dev lo root

os.system("pkill -f LogCabin --echo")
os.system("pkill -f Reconfigure --echo")
os.system("rm -rf storage")

output_file = open("output-2.csv", "w")
output_file.write("delay (ms),servers,commands (in parallel),time (sec)\n")


#server_numbers = [1, 3]
server_numbers = [5, 10, 15, 20]
command_numbers = [20, 50, 100]
delays = [10, 20, 50, 100]

os.system("python gen-config.py " + str(max(server_numbers)))

for delay in delays:
  os.system("sudo tc qdisc del dev lo root")
  x2 = os.system("sudo tc qdisc add dev lo root handle 1:0 netem delay " +str(delay) +"msec")

  if x2 != 0:
    print("Failed to set delay")
    exit(1)

  for servers in server_numbers:
    
    os.system("python run-cluster.py " + str(servers) + " --nonblocking")

    sleep(3)

    for commands in command_numbers:
      print("Configuration: servers:{}, commands:{}, delay:{}".format(servers, commands, delay))
      sleep(1)
      
      out = subprocess.check_output(["python", "commands.py", str(commands)])
      time = 0
      for line in out.splitlines():
        if line.startswith(">>>>>:"):
          time = float(line.split(":")[1])
      print("Time ", time)
      output_file.write("{},{},{},{}\n".format(delay, servers, commands, time))
      output_file.flush()
    
    sleep(3)
    os.system("pkill -f LogCabin --echo")
    os.system("pkill -f Reconfigure --echo")
    os.system("rm -rf storage")

output_file.close()