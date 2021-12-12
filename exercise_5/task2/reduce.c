#include <stdio.h>
#include <stdlib.h>
#include <mpi.h>

const int MAX_CONTRIB = 200;


int sum_slice(int start, int end) {
  int sum = 0;

  for (int i = start; i < end; i++) {
    sum += i;
  }

  return sum;
}


int main(int argc, char *argv[]) {
    int      p, my_rank, len;
    MPI_Comm comm = MPI_COMM_WORLD;
    
    //INITIALIZE MPI
    MPI_Init(&argc, &argv);
    MPI_Comm_size(MPI_COMM_WORLD, &p);
    MPI_Comm_rank(MPI_COMM_WORLD, &my_rank);
    
    int slice_start = (2000/p) * my_rank;
    int slice_end = (2000/p) * (my_rank + 1);

    if (my_rank == p -1) {
      slice_end = 2001;
    }


    int sum = sum_slice(slice_start, slice_end);
    //printf("proc %d > sum %d to %d = %d\n", my_rank, slice_start, slice_end, sum);

    int total_sum = 0;

    MPI_Reduce(&sum,&total_sum, 1, MPI_INT, MPI_SUM, 0, MPI_COMM_WORLD);


    if (my_rank == 0) {
      printf("Total sum 1 to 2000: %d\n", total_sum);
    }

    return 0;
}