filename = "logcabin-NR.conf"

config = """
serverId = NR
listenAddresses = 127.0.0.1:PORT
"""

START_PORT=5254


for i in range(20):
  port = i + START_PORT
  cfg = config.replace("NR", str(i+1)).replace("PORT", str(port))

  f = open(filename.replace("NR", str(i+1)),"w")
  f.write(cfg)
  f.close()