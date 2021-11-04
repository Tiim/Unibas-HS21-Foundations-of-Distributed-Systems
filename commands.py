import subprocess
import time


cfile = open("cluster.txt", "r")

CLUSTER=cfile.read()
cfile.close()

NUMBER=10

timeStarted = time.time()


processes = []
for i in range(NUMBER):

  cmd = ["build/Examples/TreeOps", "--cluster="+CLUSTER, "write", "/test"]
  print(" ".join(cmd))
  print(" > testdata "+ str(i) )
  p = subprocess.Popen(cmd, stdin=subprocess.PIPE)
  p.communicate(input="testdata " + str(i))
  
  processes.append(p)


print("Waiting for commands to finish")

for p in processes:
  p.wait()

timeDelta = time.time() - timeStarted
print("Finished process in "+str(timeDelta)+" seconds.")  

