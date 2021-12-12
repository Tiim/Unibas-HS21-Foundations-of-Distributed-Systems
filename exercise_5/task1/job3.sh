#!/bin/bash
#SBATCH -J job3
#SBATCH --time=00:05:00
#SBATCH --exclusive
#SBATCH --nodes=1
#SBATCH --ntasks-per-node=2
#SBATCH --cpus-per-task=1
#SBATCH --partition=xeon
#SBATCH --output=OUTPUT3.txt


module load intel
icc -O3 -fopenmp fib_opt.c -o fib_opt
srun fib_opt

