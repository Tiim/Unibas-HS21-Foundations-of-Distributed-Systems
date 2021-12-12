#!/bin/bash
#SBATCH -J job2
#SBATCH --time=00:05:00
#SBATCH --exclusive
#SBATCH --nodes=15
#SBATCH --ntasks-per-node=2
#SBATCH --cpus-per-task=1
#SBATCH --partition=xeon
#SBATCH --output=OUTPUT2.txt


module load intel
icc -O3 -fopenmp whatTheHeck.c -o whatTheHeck
srun whatTheHeck

