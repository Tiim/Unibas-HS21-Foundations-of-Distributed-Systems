#include <stdio.h>
#include <omp.h>
// In this exercise you need to correct this code. The parallel region is causing problems.//
static const int a[] = {1,2,3,4,5,6,7,8,9,10}; 
int sum(const int* arr, size_t n) {
  int s=0;
  omp_set_num_threads(15);
  #pragma omp parallel for
  for(size_t i=0; i < n; ++i) {
    
    // We modify the same value s from multiple threads.
    // Therefore it is possible that some modifications happen at the same
    // time which leads to missed modifications, and therefore a sum which is too
    // low.
    // using the atomic pragma we tell the compiler to introduce safeguards arount
    // the statement which makes the assignement atomic.
    
    #pragma omp atomic
    s += arr[i];
  }
  return s;
}
int main() {
  printf("sum: %d\n", sum(a, 10)); //Expected: 55
  return 0;
}
