import os
import subprocess
from time import sleep


os.system("pkill -f LogCabin --echo")
os.system("pkill -f Reconfigure --echo")
os.system("rm -rf storage")

output_file = open("output-3ab.csv", "w")
output_file.write("snapshot, commands, size\n")

for snapshot in [False, True]:
  os.system("python gen-config.py 5" + (" --snapshot" if snapshot else ""))
  os.system("python run-cluster.py 3 --nonblocking")


  cfile = open("cluster.txt", "r")
  CLUSTER=cfile.read()
  cfile.close()


  for i in range(6):
    os.system("python commands.py 500")
    out = subprocess.check_output(["build/Client/ServerControl", "--server="+CLUSTER, "stats", "get"])
    size = None
    for line in out.splitlines():
      if "log_bytes" in line:
        size = int(line.split(":")[1])
    output_file.write("{},{},{}\n".format(snapshot, (i+1) * 500, size))
    output_file.flush()


  os.system("pkill -f LogCabin --echo")
  os.system("pkill -f Reconfigure --echo")
  os.system("rm -rf storage")

  sleep(2)
  os.system("cat logcabin-1.conf")
  sleep(8)


output_file.close()