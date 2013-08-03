#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

#define BUFSIZE 1024

char buf[BUFSIZE];

int main(int argc, char** argv)
{
  int fd, ul, op, cl;

  fd = creat("unlinkwait.txt");
  if (fd != -1)
	printf("unlinkwait.txt was created and put in fd %d\n", fd);
  
  ul = unlink("unlinkwait.txt");
  if (ul == -1)
	printf("Couldn't unlink unlinkwait.txt\n");

  op = open("unlinkwait.txt");
  if (op == -1)
	printf("Can't open unlinkwait.txt now\n");
  else
	printf("unlinkwait.txt was opened and put in fd %d\n", op);

  cl = close(fd);
  if (cl == 0)
	printf("Closed fd %d\n", fd);
  
  ul = unlink("unlinkwait.txt");
  if (ul == 0)
	printf("Unlinked unlinkwait.txt\n");
	
  op = open("unlinkwait.txt");
  if (op != -1)
	printf("unlinkwait.txt was opened and put in fd %d\n", op);

  return 0; 
}