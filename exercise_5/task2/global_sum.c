/* File:      global_sum.c
 * Purpose:   Skeleton for MPI global sum function
 * Input:     None
 * Output:    Random values generated by processes and sum of these
 *            values.
 *
 * Algorithm:
 *    1. Each process computes a random int x from 0 to MAX_CONTRIB-1
 *    2. Processes call Global_sum function which sums the local
 *       values on process 0
 *    3. Process 0 prints global sum
 *
 * Compile:  mpicc -g -Wall global_sum.c -o global_sum
 * Run:      sbatch yourJobScript
 *
 * Notes:
 *    1. This version has no restriction on the number of processes.
 *    2. The value returned by global_sum on processes other
 *       than 0 is meaningless.
 *    3. DEBUG flag can be used to print information on pairing
 *       of processes.
 */
#include <stdio.h>
#include <stdlib.h>
#include "mpi.h"

#define TRUE 1
#define DEBUG
#define MAX_CONTRIB 200

int global_sum(int my_contrib, int my_rank, int p, MPI_Comm comm);

int main(int argc, char *argv[]) {
   
    int p, my_rank;
    MPI_Comm comm;
    int x;
    int total;
    
//INITIALIZE MPI
    MPI_Init(&argc, &argv);
    MPI_Comm_size(MPI_COMM_WORLD, &p);
    MPI_Comm_rank(MPI_COMM_WORLD, &my_rank);
    
    srandom(my_rank+1);
    x = random() % MAX_CONTRIB;
    printf("Proc %d > x = %d\n", my_rank, x);
    
    total = global_sum(x, my_rank, p, comm);
    
    //PRINT RESULT
    printf("SUM: %d", total);
    fflush(stdout);
    
    MPI_Finalize();
    return 0;
}  /* main */


/*---------------------------------------------------------------
 * Function:  global_sum
 * Purpose:   Compute global sum of values distributed across
 *            processes
 * Input args:
 *    my_contrib:  the calling process' contribution to the global sum
 *    my_rank:     the calling process' rank in the communicator
 *    p:           the number of processes in the communicator
 *    comm:        the communicator used for sends and receives
 *
 * Return val:  the sum of the my_contrib values contributed by
 *    each process.
 *
 * Algorithm:  Use tree structured communication, pairing processes
 *    to communicate.
 *
 * Notes:
 *    1. The value returned by global_sum on processes other
 *       than 0 is meaningless.
 *    2. The pairing of the processes is done using bitwise
 *       exclusive or.  Here's a table showing the rule for
 *       for bitwise exclusive or
 *           X Y X^Y
 *           0 0  0
 *           0 1  1
 *           1 0  1
 *           1 1  0
 *       Here's a table showing the process pairing with 8
 *       processes (r = my_rank, other column heads are bitmask)
 *           r     001 010 100
 *           -     --- --- ---
 *           0 000 001 010 100
 *           1 001 000  x   x
 *           2 010 011 000  x
 *           3 011 010  x   x
 *           4 100 101 110 000
 *           5 101 100  x   x
 *           6 110 111 100  x
 *           7 111 110  x   x
 */
int global_sum(int my_contrib, int my_rank, int p, MPI_Comm comm) {
    int sum = my_contrib;
    int temp;
    int partner;
    int done = 0;
    unsigned bitmask = (unsigned) 1;
    int my_pass = 0;

    MPI_Status stat;
    
   
    while (!done) {
        partner = my_rank ^ bitmask;

        if (partner >= p || bitmask > p) {
          break;
        }

        #   ifdef DEBUG
          my_pass += 1;
          printf("Proc %d > partner = %d, bitmask = %d, pass = %d\n",
              my_rank, partner, bitmask, my_pass);
          fflush(stdout);
        #   endif


        if (partner < my_rank) {
            // send to partner
            printf("Proc %d > sending to %d\n", my_rank, partner);
            MPI_Send(&sum, 1, MPI_INT, partner, 0, MPI_COMM_WORLD);
            printf("Proc %d > sent! to %d\n", my_rank, partner);
            break;
        } else {
            // recv from partner
            printf("Proc %d > receiving from %d\n", my_rank, partner);
            MPI_Recv(&temp, 1, MPI_INT, partner, 0, MPI_COMM_WORLD, &stat);
            printf("Proc %d > received! from %d\n", my_rank, partner);
            sum += temp;
        }

        bitmask <<= 1;
    }
    
    /* Valid only on 0 */
    return sum;
}  /* Global_sum */