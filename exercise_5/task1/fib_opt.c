
#include<stdio.h>
#include<omp.h>
int fib(int n) {
  if (n <= 1){
  return n;
  }
  int x,y;
  #pragma omp task shared(x)
  x = fib(n-1);
  #pragma omp task shared(y)
  y = fib(n-2);
  #pragma omp taskwait
  return x+y;
}
int main () {
  for(int i=0;i<12;i++)
    printf("%d ", fib(i));
      return 0;
}
