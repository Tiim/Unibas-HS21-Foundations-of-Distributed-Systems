/* File:     ring_global_sum.c
 *
 * Purpose:  Program that uses MPI to implement a global
 *           sum
 *
 * Input:    None.
 * Output:   Random values generated by processes and sum of random
 *           values on each process.
 *
 * Compile:  mpicc -g -Wall global_sum_rp.c -o sum
 * Run:      sbatch yourJobScript
 *
 * Notes:
 *    1.  The result returned by all the processes should be valid.
 *    2.  The global_sum function uses a ring-pass.
 */
#include <stdio.h>
#include <stdlib.h>
#include <mpi.h>

const int MAX_CONTRIB = 200;

int global_sum(int my_contrib, int my_rank, int p, MPI_Comm comm);
void print_results(char title[], int value, int my_rank, int p,
                   MPI_Comm comm);

int main(void) {
    int      p, my_rank, len;
    MPI_Comm comm;
    int      my_contrib;
    int      sum;
    char name[MPI_MAX_PROCESSOR_NAME];
    
    //INITIALIZE MPI
    
    /* Generate a random int */
    srandom(my_rank);
    my_contrib = random() % MAX_CONTRIB;
    
    print_results("Process Values", my_contrib, my_rank, p, comm);
    
    sum = global_sum(my_contrib, my_rank, p, comm);
    
    print_results("Process Totals", sum, my_rank, p, comm);
    
    return 0;
}
/*---------------------------------------------------------------
 * Function:  print_results
 * Purpose:   Gather an int from each process onto process 0 and
 *            print the values.
 * In args:
 *    title:  the title of the output
 *    value:  the int contributed by each process
 *    my_rank, p, comm:  the usual MPI values
 */
void print_results(char title[], int value, int my_rank, int p, MPI_Comm comm) {
    int* vals = NULL, q;
    
    if (my_rank == 0) {
        vals = malloc(p*sizeof(int));
        //TODO
        printf("%s:\n", title);
        for (q = 0; q < p; q++)
            printf("Proc %d > %d\n", q, vals[q]);
        printf("\n");
        free(vals);
    } else {
        //TODO
    }
}  /* print_results */


/*-----------------------------------------------------------------
 * Function:    global_sum
 * Purpose:     Compute the global sum of ints stored on the processes
 *
 * Input args:  my_contrib = process's contribution to the global sum
 *              my_rank = process's rank
 *              p = number of processes
 *              comm = communicator
 * Return val:  Sum of each process's my_contrib:  valid on all
 *              processes
 *
 */
int global_sum(int my_contrib, int my_rank, int p, MPI_Comm comm) {
    int sum, temp, i;
    int dest = //TODO;
    int source = //TODO;
    
    temp = sum = my_contrib;
    for () {
        //TODO
    }
    
    return sum;
}  /* Global_sum */
