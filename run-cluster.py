import subprocess
import os


os.system("rm -rf storage")
os.system("pkill -f LogCabin --echo")
os.system("pkill -f Reconfigure --echo")

number = 4

for i in range(1, number+1):
  cmd = ["build/LogCabin", "--config", "logcabin-" + str(i) + ".conf"]

  if i == 1:
    print(" ".join(cmd + ["--bootstrap"]))
    p = subprocess.Popen(cmd + ["--bootstrap"])
    p.wait()

  print(" ".join(cmd))
  subprocess.Popen(cmd)


allservers = ["127.0.0.1:"+str(port) for port in range(5254, 5254+number)]
allservers_comma = ",".join(allservers)

cmd = ["build/Examples/Reconfigure", "--cluster=" + allservers_comma, "set"]
cmd.extend(allservers)

print(" ".join(cmd))
subprocess.Popen(cmd)



raw_input("Press Enter to exit...")

os.system("rm -rf storage")
os.system("pkill -f LogCabin --echo")
os.system("pkill -f Reconfigure --echo")