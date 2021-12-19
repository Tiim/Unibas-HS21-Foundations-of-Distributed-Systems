#!/bin/bash
#SBATCH -J mandel
#SBATCH --time=00:05:00
#SBATCH --exclusive
#SBATCH --nodes=4
#SBATCH --ntasks-per-node=2
#SBATCH --cpus-per-task=1
#SBATCH --partition=xeon
#SBATCH --output=OUTPUT-mpi8.txt


module load intel
mpiicc -O3 -fopenmp ../task1/mandel.c -o mandel_par
srun mandel_par 10000 1024 0 0 0.75
srun mandel_par 10000 1024 0 0 0.75
srun mandel_par 10000 1024 0 0 0.75
srun mandel_par 10000 1024 0 0 0.75
srun mandel_par 10000 1024 0 0 0.75
