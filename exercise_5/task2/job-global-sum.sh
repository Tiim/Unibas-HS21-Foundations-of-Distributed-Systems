#!/bin/bash
#SBATCH -J job1
#SBATCH --time=00:05:00
#SBATCH --exclusive
#SBATCH --nodes=2
#SBATCH --ntasks-per-node=1
#SBATCH --cpus-per-task=1
#SBATCH --partition=xeon
#SBATCH --output=OUTPUT.txt


module load intel
mpiicc -O3 global_sum.c -o hw
srun hw