#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

#define BUFSIZE 1024

char buf[BUFSIZE];

int main(int argc, char** argv)
{
  int fd, amount;
  char buf2[BUFSIZE];
  
  fd = open("sup.txt");
  printf("sup.txt was opened and put in fd %d\n", fd);
  amount = read(fd, buf2, BUFSIZE);
  write(1, buf2, amount);
  
  return 0;
}