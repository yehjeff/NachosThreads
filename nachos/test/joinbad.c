#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

#define BUFSIZE 1024

char buf[BUFSIZE];

int main(int argc, char** argv)
{
  int fd, exit;
  int* status;

  exit = join(20, status);
  if (exit == -1)
	printf("Didn't join properly");
  else
	printf("Process 20 joined");

  fd = creat("joinbad.txt");
  

  return 0; 
}