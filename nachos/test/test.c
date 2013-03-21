#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"
#include "syscall.h"

#define BUFSIZE 1024

char buf[BUFSIZE];

int main(int argc, char** argv)
{
  int fd, amount;
  char buf2[BUFSIZE];
  
  fd = creat("sup.txt");
  printf("sup.txt was created\n");
  buf[0] = 's';
  buf[1] = 'u';
  buf[2] = 'p';
  write(fd, buf, 3);
  
  close(fd);
  fd = unlink("sup.txt");
  printf("%d", fd);
  printf("hello");
  if (fd == 0) {
     printf("Unlinked sup.txt");
  }

  return 0; 
}

