#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

#define BUFSIZE 1024

char buf[BUFSIZE];

int main(int argc, char** argv)
{
  int fd, amount;

  fd = creat("sup.txt");
  printf("sup.txt was created and put in fd %d\n", fd);

  buf[0] = 's';
  buf[1] = 'u';
  buf[2] = 'p';
  
  write(fd, buf, 3);
  close(fd);
  printf("Closed fd %d\n", fd);

  return 0; 
}