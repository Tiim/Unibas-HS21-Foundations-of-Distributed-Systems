import subprocess
import time
import sys

cfile = open("cluster.txt", "r")

CLUSTER=cfile.read()
cfile.close()


NUMBER=1800
if len(sys.argv) > 1:
  NUMBER=int(sys.argv[1])


timeStarted = time.time()


processes = []

# Run all commands, don't wait for one command to stop before the next one is executed
for i in range(NUMBER):

  cmd = ["build/Examples/TreeOps", "--cluster="+CLUSTER, "write", "/test"]
  print(" ".join(cmd))
  print(" > testdata "+ str(i) )
  p = subprocess.Popen(cmd, stdin=subprocess.PIPE)
  p.stdin.write("testdata " + str(i))
  p.stdin.close()
  processes.append(p)


print("Waiting for commands to finish")

for p in processes:
  p.wait()

timeDelta = time.time() - timeStarted
print("Finished process in "+str(timeDelta)+" seconds.")
print(">>>>>:" + str(timeDelta))