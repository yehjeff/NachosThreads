#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"
#include "syscall.h"

#define BUFSIZE 1024

char buf[BUFSIZE];

int main(int argc, char** argv)
{
  int fd1, fd2, amount;
  char buf2[BUFSIZE];
  
  fd1 = creat("hey.txt");
  buf[0] = 'h';
  buf[1] = 'e';
  buf[2] = 'y';
  write(fd1, buf2, 3);
  amount = read(fd1, buf, BUFSIZE);
  write(1, buf, amount);
  
  fd2 = creat("hey.txt");
  amount = read(fd2, buf, BUFSIZE);
  write(1, buf, amount);
  
  int closed1 = close(fd1);
  int closed2 = close(fd2);

  return 0; 
}

