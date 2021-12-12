#include<stdio.h>
#include<omp.h>
int fib(int n) {
  if (n <= 1){
  return n;
  }
  return fib(n-1) + fib(n-2);
}
int main () {
  for(int i=0;i<12;i++)
    printf("%d ", fib(i));
      return 0;
}

