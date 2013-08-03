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
  printf("hey.txt was opened and put in fd %d\n", fd1);
  amount = read(fd1, buf2, BUFSIZE);
  write(1, buf2, amount);
  
  fd2 = creat("hey.txt");
  printf("hey.txt was created and put in fd %d\n", fd2);
  amount = read(fd2, buf2, BUFSIZE);
  write(1, buf2, amount);
    
  int closed1 = close(fd1);
  printf("Closed fd %d\n", fd1);
  int closed2 = close(fd2);
  printf("Closed fd %d\n", fd2);
  
  fd1 = unlink("hey.txt");
  if (fd1 == 0)
	printf("Unlinked hey.txt once\n");
  fd2 = unlink("hey.txt");
  if (fd2 == 0)
	printf("Unlinked hey.txt twice\n");

  return 0; 
}