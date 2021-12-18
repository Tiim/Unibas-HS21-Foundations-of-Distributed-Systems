#!/bin/bash
#SBATCH -J mandel
#SBATCH --time=00:05:00
#SBATCH --exclusive
#SBATCH --nodes=2
#SBATCH --ntasks-per-node=2
#SBATCH --cpus-per-task=20
#SBATCH --partition=xeon
#SBATCH --output=OUTPUT.txt


module load intel
mpiicc -O3 -fopenmp mandel.c -o mandel
srun mandel 10000 1024 0 0 0.75