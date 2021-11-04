Vagrant.configure("2") do |config|
  config.vm.box = "ubuntu/xenial64"

    config.vm.provider "virtualbox" do |v|
      v.memory = 3072
      v.cpus = 4
    end
  
  config.vm.provision "shell", inline: <<-SHELL
    
    sudo apt-get update -qq
    sudo apt-get install -y g++
    sudo apt-get install -y protobuf-compiler libprotobuf-dev
    sudo apt-get install -y libcrypto++-dev
    sudo apt-get --no-install-recommends install -y doxygen
    sudo apt-get install -y rpm
    sudo apt-get install -y scons
  SHELL

  config.vm.provision "shell", privileged: false, inline: <<-SHELL
    git clone https://github.com/logcabin/logcabin.git
    
    cd logcabin
    git submodule update --init

    scons
  SHELL

  config.ssh.forward_x11 = true
  config.ssh.forward_agent = true

end