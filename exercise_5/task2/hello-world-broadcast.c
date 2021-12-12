
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "mpi.h"
#include <omp.h>

void send_chars(int rank, int tasks) {
    char buffer[50];
    MPI_Status status;
    MPI_Request rq;

    if (rank == 0) {
        strcpy(buffer, "hello world");
    }
    MPI_Bcast(buffer, 50, MPI_CHAR, 0, MPI_COMM_WORLD);
    
    // send message to everyone, forward if its our turn    
    for (int i = 1; i < tasks; i++) {
        MPI_Bcast(buffer, 50, MPI_CHAR, i, MPI_COMM_WORLD);
        if (i == tasks - 1 && rank == tasks - 1) {
            printf("RECEIVED BROADCAST %s\n", buffer);
        }
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



