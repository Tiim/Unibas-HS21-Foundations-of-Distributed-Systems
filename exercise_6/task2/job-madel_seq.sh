#!/bin/bash
#SBATCH -J mandel
#SBATCH --time=00:05:00
#SBATCH --exclusive
#SBATCH --nodes=5
#SBATCH --ntasks-per-node=1
#SBATCH --cpus-per-task=1
#SBATCH --partition=xeon
#SBATCH --output=OUTPUT.txt


module load intel
mpiicc -O3 -fopenmp mandel_seq.c -o mandel
srun mandel 10000 1024 0 0 0.75