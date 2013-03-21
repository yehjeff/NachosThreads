#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

#define BUFSIZE 1024

char buf[BUFSIZE];

int main(int argc, char** argv)
{
  int fd, amount;

  fd = creat("sup.txt");
  printf("sup.txt was created\n");

  buf[0] = 's';
  buf[1] = 'u';
  buf[2] = 'p';
  
  write(fd, buf, 3);
  close(fd);

  return 0; 
}