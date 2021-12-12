
#include <stdio.h>
#include <stdlib.h>
#include "mpi.h"
#include <omp.h>

void send_chars(int rank, int tasks) {
    char buffer[50];
    MPI_Status status;
    MPI_Request rq;
    if (rank == 0) {
        // send message
        MPI_Isend("Hello World", 12, MPI_CHAR, 1, 0, MPI_COMM_WORLD, &rq);
        MPI_Wait(&rq, &status);
    } else if ( rank > 0 && rank < tasks - 1 ) {
        // receive message
        // send message
        MPI_Irecv(&buffer, 50, MPI_CHAR, rank - 1, 0, MPI_COMM_WORLD, &rq);
        MPI_Wait(&rq, &status);
        MPI_Isend(&buffer, 50, MPI_CHAR, rank + 1, 0, MPI_COMM_WORLD, &rq);
        MPI_Wait(&rq, &status);
    } else {
        // receive message
        // print messge
        MPI_Irecv(&buffer, 50, MPI_CHAR, rank - 1, 0, MPI_COMM_WORLD, &rq);
        MPI_Wait(&rq, &status);
        printf("RECEIVED NON BLOCKING: %s\n", buffer);
    }
}

int main(int argc, char *argv[])
{
    int npes, rank;
    MPI_Init(&argc, &argv);
    MPI_Comm_size(MPI_COMM_WORLD, &npes);
    MPI_Comm_rank(MPI_COMM_WORLD, &rank);
    //printf("From process %d out of %d!\n", rank, npes);

    send_chars(rank, npes);

    //printf("DONE! (rank: %d)\n", rank);

    MPI_Finalize();
    return 0;
}



