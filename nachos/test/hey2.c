#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

#define BUFSIZE 1024

char buf[BUFSIZE];

int main(int argc, char** argv)
{
  int fd1, fd2, amount;
  char buf2[BUFSIZE];
  
  fd1 = open("hey.txt");
  amount = read(fd1, buf2, BUFSIZE);
  write(1, buf2, amount);
  
  fd2 = creat("hey.txt");
  amount = read(fd2, buf2, BUFSIZE);
  write(1, buf2, amount);
    
  int closed1 = close(fd1);
  int closed2 = close(fd2);

  return 0; 
}