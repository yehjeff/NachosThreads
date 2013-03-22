#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

#define BUFSIZE 1024

char buf[BUFSIZE];

int main(int argc, char** argv)
{
  int fd;
  
  fd = open("sup.txt");  
  printf("sup.txt was opened and put in fd %d\n", fd);
  close(fd);
  printf("Closed fd %d\n", fd);
  fd = unlink("sup.txt");
  if (fd == 0) {
     printf("Unlinked sup.txt\n");
  }

  return 0; 
}