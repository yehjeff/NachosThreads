#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

#define BUFSIZE 1024

char buf[BUFSIZE];

int main(int argc, char** argv)
{
  int fd, amount;

  fd = creat("hey.txt");
  printf("hey.txt was created\n");

  buf[0] = 'h';
  buf[1] = 'e';
  buf[2] = 'y';
  
  write(fd, buf, 3);
  close(fd);

  return 0; 
}