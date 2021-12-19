#!/bin/bash
#SBATCH -J mandel
#SBATCH --time=00:05:00
#SBATCH --exclusive
#SBATCH --nodes=1
#SBATCH --ntasks-per-node=1
#SBATCH --cpus-per-task=8
#SBATCH --partition=xeon
#SBATCH --output=OUTPUT-threads8.txt


module load intel
mpiicc -O3 -fopenmp ../task1/mandel.c -o mandel_par
srun mandel_par 10000 1024 0 0 0.75
srun mandel_par 10000 1024 0 0 0.75
srun mandel_par 10000 1024 0 0 0.75
srun mandel_par 10000 1024 0 0 0.75
srun mandel_par 10000 1024 0 0 0.75
