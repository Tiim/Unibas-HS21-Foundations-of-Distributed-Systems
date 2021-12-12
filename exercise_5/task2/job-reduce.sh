#!/bin/bash
#SBATCH -J job1
#SBATCH --time=00:05:00
#SBATCH --exclusive
#SBATCH --nodes=3
#SBATCH --ntasks-per-node=2
#SBATCH --cpus-per-task=1
#SBATCH --partition=xeon
#SBATCH --output=OUTPUT.txt


module load intel
mpiicc -O3 reduce.c -o hw
srun hw