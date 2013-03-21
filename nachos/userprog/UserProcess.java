package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
	/**
	 * Allocate a new process.
	 */
	public UserProcess() {

		processCountLock.acquire();
		processID = processCount++;
		processCountLock.release();

		numProcessesAliveLock.acquire();
		numProcessesAlive++;
		numProcessesAliveLock.release();

		fileArray = new OpenFile[16];
		numFreeFileDesc = 16;

		addToFileArray(UserKernel.console.openForReading());
		addToFileArray(UserKernel.console.openForWriting());

	}

	/**
	 * Allocate and return a new process of the correct class. The class name
	 * is specified by the <tt>nachos.conf</tt> key
	 * <tt>Kernel.processClassName</tt>.
	 *
	 * @return	a new process of the correct class.
	 */
	public static UserProcess newUserProcess() {
		return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
	}

	/**
	 * Execute the specified program with the specified arguments. Attempts to
	 * load the program, and then forks a thread to run it.
	 *
	 * @param	name	the name of the file containing the executable.
	 * @param	args	the arguments to pass to the executable.
	 * @return	<tt>true</tt> if the program was successfully executed.
	 */
	public boolean execute(String name, String[] args) {
		if (!load(name, args))
			return false;

		new UThread(this).setName(name).fork();

		return true;
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		Machine.processor().setPageTable(pageTable);
	}

	/**
	 * Read a null-terminated string from this process's virtual memory. Read
	 * at most <tt>maxLength + 1</tt> bytes from the specified address, search
	 * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
	 * without including the null terminator. If no null terminator is found,
	 * returns <tt>null</tt>.
	 *
	 * @param	vaddr	the starting virtual address of the null-terminated
	 *			string.
	 * @param	maxLength	the maximum number of characters in the string,
	 *				not including the null terminator.
	 * @return	the string read, or <tt>null</tt> if no null terminator was
	 *		found.
	 */
	public String readVirtualMemoryString(int vaddr, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength+1];

		int bytesRead = readVirtualMemory(vaddr, bytes);

		for (int length=0; length<bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}

		return null;
	}

	/**
	 * Transfer data from this process's virtual memory to all of the specified
	 * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 *
	 * @param	vaddr	the first byte of virtual memory to read.
	 * @param	data	the array where the data will be stored.
	 * @return	the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data) {
		return readVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from this process's virtual memory to the specified array.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no
	 * data could be copied).
	 *
	 * @param	vaddr	the first byte of virtual memory to read.
	 * @param	data	the array where the data will be stored.
	 * @param	offset	the first byte to write in the array.
	 * @param	length	the number of bytes to transfer from virtual memory to
	 *			the array.
	 * @return	the number of bytes successfully transferred.
	 * 
	 * PROJ2: modifying to support multiprogramming
	 */
	public int readVirtualMemory(int vaddr, byte[] data, int offset,
			int length) {
		Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);
		
		/*
		addrLegit(vaddr, length);
		
		byte[] memory = Machine.processor().getMemory();
		int startVAddr = vaddr;
		int startOffset = vaddr % pageSize;
		int endVAddr = Math.min(startVAddr + length, pageTable.length*pageSize);
		int startVPN = startVAddr / pageSize;
		int endVPN = endVAddr / pageSize;
		int totalToRead = endVAddr - startVAddr;
		int leftToRead = totalToRead;
		
		for (int i = startVPN; i <= endVPN; i++){
			TranslationEntry PTE = pageTable[i];
			if (!PTE.valid)
				return length-leftToRead;
			int PPN = PTE.ppn;
			if (i == startVPN) 
				System.arraycopy(memory, PPN*pageSize+startOffset, data, offset, Math.min(leftToRead, pageSize));
			else 
				System.arraycopy(memory, PPN*pageSize, data, offset, Math.min(leftToRead, pageSize));
			leftToRead -= Math.min(leftToRead, pageSize);
			offset += Math.min(leftToRead, pageSize);
			
		}
		return length;
		*/

		byte[] memory = Machine.processor().getMemory();
	    // for now, just assume that virtual addresses equal physical addresses
	    if (vaddr < 0 || vaddr >= memory.length)
	      return 0;
	
	    int amount = Math.min(length, memory.length-vaddr);
	    System.arraycopy(memory, vaddr, data, offset, amount);
	
	    return amount;
	    
	}

	/**
	 * Transfer all data from the specified array to this process's virtual
	 * memory.
	 * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 *
	 * @param	vaddr	the first byte of virtual memory to write.
	 * @param	data	the array containing the data to transfer.
	 * @return	the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data) {
		return writeVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from the specified array to this process's virtual memory.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no
	 * data could be copied).
	 *
	 * @param	vaddr	the first byte of virtual memory to write.
	 * @param	data	the array containing the data to transfer.
	 * @param	offset	the first byte to transfer from the array.
	 * @param	length	the number of bytes to transfer from the array to
	 *			virtual memory.
	 * @return	the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data, int offset,
			int length) {
		//System.out.println("\nWriting virtual memory...");
		Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

		
		//addrLegit(vaddr, length);
		byte[] memory = Machine.processor().getMemory();

		/*
		int startVAddr = vaddr + offset;
		int startOffset = (vaddr + offset) % Processor.pageSize;
		int endVAddr = Math.min(startVAddr + length, pageTable.length*pageSize);
		int startVPN = startVAddr / pageSize;
		int endVPN = endVAddr / pageSize;
		int totalToWrite = endVAddr - startVAddr;
		int leftToWrite = totalToWrite;

		for (int i = startVPN; i <= endVPN; i++){
			TranslationEntry PTE = pageTable[i];
			if (!PTE.valid || PTE.readOnly) {
				System.out.println("\nInvalid page table entry");
				return length - leftToWrite; 			
			}
			int PPN = PTE.ppn;
			if (i == startVPN)
				System.arraycopy( data,offset,memory, PPN*pageSize+startOffset, Math.min(leftToWrite, pageSize));
			else
				System.arraycopy(data, offset,memory,PPN*pageSize, Math.min(leftToWrite, pageSize));
			leftToWrite -= Math.min(leftToWrite, pageSize);
			offset += Math.min(leftToWrite, pageSize);
		}
		System.out.println("\nNum bytes transfered: " + length);
		return length;
		*/
		
		// for now, just assume that virtual addresses equal physical addresses
		    if (vaddr < 0 || vaddr >= memory.length)
		      return 0;
		
		    int amount = Math.min(length, memory.length-vaddr);
		    System.arraycopy(data, offset, memory, vaddr, amount);
		
		    return amount;
	}

	/**
	 * Load the executable with the specified name into this process, and
	 * prepare to pass it the specified arguments. Opens the executable, reads
	 * its header information, and copies sections and arguments into this
	 * process's virtual memory.
	 *
	 * @param	name	the name of the file containing the executable.q
	 * @param	args	the arguments to pass to the executable.
	 * @return	<tt>true</tt> if the executable was successfully loaded.
	 */
	private boolean load(String name, String[] args) {
		Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

		OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
		if (executable == null) {
			Lib.debug(dbgProcess, "\topen failed");
			return false;
		}

		try {
			coff = new Coff(executable);
		}
		catch (EOFException e) {
			executable.close();
			Lib.debug(dbgProcess, "\tcoff load failed");
			return false;
		}

		// make sure the sections are contiguous and start at page 0
		numPages = 0;
		for (int s=0; s<coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			if (section.getFirstVPN() != numPages) {
				coff.close();
				Lib.debug(dbgProcess, "\tfragmented executable");
				return false;
			}
			numPages += section.getLength();
		}

		// make sure the argv array will fit in one page
		byte[][] argv = new byte[args.length][];
		int argsSize = 0;
		for (int i=0; i<args.length; i++) {
			argv[i] = args[i].getBytes();
			// 4 bytes for argv[] pointer; then string plus one for null byte
			argsSize += 4 + argv[i].length + 1;
		}
		if (argsSize > pageSize) {
			coff.close();
			Lib.debug(dbgProcess, "\targuments too long");
			return false;
		}

		// program counter initially points at the program entry point
		initialPC = coff.getEntryPoint();	

		// next comes the stack; stack pointer initially points to top of it
		numPages += stackPages;
		initialSP = numPages*pageSize;

		// and finally reserve 1 page for arguments
		numPages++;
		
		if (!loadSections())
			return false;

		// store arguments in last page
		int entryOffset = (numPages-1)*pageSize;
		int stringOffset = entryOffset + args.length*4;

		this.argc = args.length;
		this.argv = entryOffset;

		for (int i=0; i<argv.length; i++) {
			byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
			Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
			entryOffset += 4;
			Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
					argv[i].length);
			stringOffset += argv[i].length;
			Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
			stringOffset += 1;
		}
		System.out.println("\nLoad successful");
		return true;
	}

	/**
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be
	 * run (this is the last step in process initialization that can fail).
	 *
	 * @return	<tt>true</tt> if the sections were successfully loaded.
	 */
	protected boolean loadSections() {

		UserKernel.freePhysicalPagesLock.acquire();

		if (numPages > UserKernel.freePhysicalPages.size()) {
			coff.close();
			UserKernel.freePhysicalPagesLock.release();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
			return false;
		}

		// load sections

		int entriesLoadedSoFar = 0;
		pageTable = new TranslationEntry[numPages];
		for (int s=0; s<coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			for (int i=0; i<section.getLength(); i++) {
				int vpn = section.getFirstVPN()+i;
				int newPPN = UserKernel.freePhysicalPages.pop();
				pageTable[vpn] = new TranslationEntry(vpn, newPPN, true, section.isReadOnly(), false, false);
				section.loadPage(i, newPPN);
				entriesLoadedSoFar += 1;
				
				// for now, just assume virtual addresses=physical addresses
				//section.loadPage(i, vpn);
			}
		}

		for (int i = entriesLoadedSoFar; i < entriesLoadedSoFar + stackPages + 1 ; i++){
			int vpn = i;
			int newPPN = UserKernel.freePhysicalPages.pop();
			pageTable[vpn] = new TranslationEntry(vpn, newPPN, true, false, false, false);

		}
		UserKernel.freePhysicalPagesLock.release();
		return true;
		}
		
	

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
	}    

	/**
	 * Initialize the processor's registers in preparation for running the
	 * program loaded into this process. Set the PC register to point at the
	 * start function, set the stack pointer register to point at the top of
	 * the stack, set the A0 and A1 registers to argc and argv, respectively,
	 * and initialize all other registers to 0.
	 */
	public void initRegisters() {
		Processor processor = Machine.processor();

		// by default, everything's 0
		for (int i=0; i<processor.numUserRegisters; i++)
			processor.writeRegister(i, 0);

		// initialize PC and SP according
		processor.writeRegister(Processor.regPC, initialPC);
		processor.writeRegister(Processor.regSP, initialSP);

		// initialize the first two argument registers to argc and argv
		processor.writeRegister(Processor.regA0, argc);
		processor.writeRegister(Processor.regA1, argv);
	}

	/**
	 * Handle the halt() system call. 
	 */
	private int handleHalt() {
		if (processID != 0)
			return -1;

		Machine.halt();

		Lib.assertNotReached("Machine.halt() did not halt machine!");
		return 0;
	}


	private static final int
	syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9;

	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 *
	 * <table>
	 * <tr><td>syscall#</td><td>syscall prototype</td></tr>
	 * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
	 * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
	 * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
	 * 								</tt></td></tr>
	 * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
	 * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
	 * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
	 * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
	 *								</tt></td></tr>
	 * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
	 *								</tt></td></tr>
	 * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
	 * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
	 * </table>
	 * 
	 * @param	syscall	the syscall number.
	 * @param	a0	the first syscall argument.
	 * @param	a1	the second syscall argument.
	 * @param	a2	the third syscall argument.
	 * @param	a3	the fourth syscall argument.
	 * @return	the value to be returned to the user.
	 */
	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		try {
			switch (syscall) {
			case syscallHalt:
				return handleHalt();
			case syscallExit:
				return handleExit(a0);
			case syscallExec:
				return handleExec(a0,a1,a2);
			case syscallJoin:
				return handleJoin(a0,a1);        
			case syscallCreate:
				return handleCreate(a0);
			case syscallOpen:
				return handleOpen(a0);
			case syscallRead:
				return handleRead(a0,a1,a2);
			case syscallWrite:
				return handleWrite(a0,a1,a2);
			case syscallClose:
				return handleClose(a0);
			case syscallUnlink:
				return handleUnlink(a0);

			default:
				Lib.debug(dbgProcess, "Unknown syscall " + syscall);
				//			Lib.assertNotReached("Unknown system call!");
				this.exitingAbnormally = true;					// is this correct? idk
				handleExit(-1);
			}
		} catch (Exception e) {
			handleExit(-1);
		}
		return 0;
	}

	/**
	 * Handle a user exception. Called by
	 * <tt>UserKernel.exceptionHandler()</tt>. The
	 * <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 *
	 * @param	cause	the user exception that occurred.
	 */
	public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
		case Processor.exceptionSyscall:
			int result = handleSyscall(processor.readRegister(Processor.regV0),
					processor.readRegister(Processor.regA0),
					processor.readRegister(Processor.regA1),
					processor.readRegister(Processor.regA2),
					processor.readRegister(Processor.regA3)
					);
			processor.writeRegister(Processor.regV0, result);
			processor.advancePC();
			break;


		default:
			Lib.debug(dbgProcess, "Unexpected exception: " + Processor.exceptionNames[cause]);
			this.exitingAbnormally = true;
			handleExit(162);
			Lib.assertNotReached("Unexpected exception");
		}

	}

	private int handleExit(int status){
		for (OpenFile file : fileArray){
			if (file != null)
				file.close();
		}
		unloadSections();
		if (this.parentProcess != null){
			this.parentProcess.childrenExitStatuses.put(this.processID, status);
			if (this.exitingAbnormally)
				this.parentProcess.childrenAbnormallyExited.add(this.processID);
		}
		numProcessesAliveLock.acquire();
		numProcessesAlive -= 1;

		if (numProcessesAlive == 0){
			numProcessesAliveLock.release();
			Kernel.kernel.terminate();
		} else {
			numProcessesAliveLock.release();
			KThread.finish();
		}
		return 0;
	}

	private int handleExec(int filenameAddr, int argc, int argv){
		if (argc < 0 || filenameAddr < 0 ){
			return -1;
		}
		String filename = readVirtualMemoryString(filenameAddr, 255);
		byte[] byteWordAddr = new byte[4];
		int intWordAddr;
		String[] realArgs = new String[argc];
		for (int i = 0; i < argc; i++){		
			readVirtualMemory(argv + i*4, byteWordAddr, 0, 4);
			intWordAddr = Lib.bytesToInt(byteWordAddr,0);
			realArgs[i] = readVirtualMemoryString(intWordAddr, 255);		//is programname supposed to be in this list?
		}
		if (filename.length() < 6 ||  filename.substring(filename.length()-5) == ".coff") //file name has to end with .coff
			return -1;
		UserProcess child = newUserProcess();
		this.childrenExitStatuses.put(child.processID, null);
		this.childrenProcesses.add(child);
		child.parentProcess = this;

		if (!child.execute(filename, realArgs))
			return -1;
		return child.processID;
	}

	private int handleJoin(int processID, int statusAddr){
		if (!this.childrenExitStatuses.containsKey(processID) || statusAddr < 0)
			return -1;
		Integer childExitStatus = childrenExitStatuses.get(processID);
		UserProcess childProcess = null;
		if (childExitStatus == null){
			for (UserProcess process : this.childrenProcesses){
				if (process.processID == processID)
					childProcess = process;
			}
			childProcess.parentProcess = this;
			KThread.sleep();
		}

		childExitStatus = this.childrenExitStatuses.get(processID);
		this.childrenExitStatuses.remove(processID);
		byte[] statusByte = Lib.bytesFromInt(childExitStatus);
		writeVirtualMemory(statusAddr,statusByte,0,4);
		if (this.childrenAbnormallyExited.contains(processID))
			return -1;
		else
			return 0;
	}

	private int handleCreate(int stringAddr) {
		if (numFreeFileDesc == 0 || !addrLegit(stringAddr, 256))
			return -1;
		String name = readVirtualMemoryString(stringAddr,255);
		//		if (!FileSystem.checkName(name))
		//			return -1;
		OpenFile newFile = UserKernel.fileSystem.open(name, true);
		return addToFileArray(newFile);        //returns fileDescriptor
	}

	private int handleOpen(int stringAddr) {
		if (numFreeFileDesc == 0 || !addrLegit(stringAddr, 256))
			return -1;
		String name = readVirtualMemoryString(stringAddr,255);
		//		if (!fileSystem.checkName(name))
		//			return -1;
		OpenFile newFile = UserKernel.fileSystem.open(name, false);
		if (newFile == null)
			return -1;
		return addToFileArray(newFile);    //returns fileDescriptor
	}

	private int handleRead(int fd, int bufferAddr, int size) {
		if (fd < 0 || bufferAddr < 0 || size < 0 || fileArray[fd]==null	|| !addrLegit(bufferAddr, size))
			return -1;
		OpenFile file = fileArray[fd];			
		byte[] buffer = new byte[pageSize];
		int numBytesRead = 0;
		int numBytesWritten = 0;
		int sizeLeftToRead = size;
		for (int i = 0; i < size/pageSize+1; i++) {
			numBytesRead = file.read(buffer, 0, Math.min(sizeLeftToRead, pageSize));			
			sizeLeftToRead -= numBytesRead;
			numBytesWritten += writeVirtualMemory(bufferAddr, buffer, 0, numBytesRead);
		}
		return size - sizeLeftToRead;	// changed
	}

	private int handleWrite(int fd, int bufferAddr, int size) {
		if (fd < 0 || size < 0 || fileArray[fd] == null || !addrLegit(bufferAddr, size))
			return -1;
		byte[] buffer = new byte[pageSize];
		OpenFile file = fileArray[fd];
		int numBytesRead = 0;
		int numBytesWritten = 0;
		int sizeLeftToRead = size;
		for (int i = 0; i < size/pageSize+1; i++) {
			numBytesRead = readVirtualMemory(bufferAddr, buffer, 0, Math.min(sizeLeftToRead, pageSize));
			sizeLeftToRead -= numBytesRead;
			numBytesWritten += file.write(buffer, 0, numBytesRead);		// changed and lines above
		}
		if (numBytesWritten < size)
			return -1;
		return numBytesWritten;
	}

	private int handleClose(int fd) {
		if (fd < 0 || fileArray[fd] == null)
			return -1;
		OpenFile file = fileArray[fd];
		fileArray[fd] = null;
		numFreeFileDesc++;
		file.close();
		return 0;
	}

	private int handleUnlink(int stringAddr) {
		if (!addrLegit(stringAddr, 256))
			return -1;
		String name = readVirtualMemoryString(stringAddr, 256);
		//		if (!fileSystem.checkName(name))
		//			return -1;
		if (UserKernel.fileSystem.remove(name))
			return 0;
		else
			return -1;
	}


	/**
	 * COMMENT LATER
	 *
	 * @param	newFile		the new file to be added in
	 * @return	<tt>i</tt> if the file was successfully added.
	 */
	private int addToFileArray(OpenFile newFile) {

		for (int i = 0; i < fileArray.length; i++) {
			if (fileArray[i] == null) {
				fileArray[i] = newFile;
				numFreeFileDesc--;
				return i;
			}
		}
		return -1;        //fail, but should be checked in error check
	}

	/**
	 * COMMENT LATER
	 *
	 * @param	addr		the address to be checked
	 * @param	count		the amount to be read/written
	 * @return	<tt>true</tt> if the address can be read correctly.
	 */
	private boolean addrLegit(int addr, int count) {
		if (addr + count <= pageTable.length * pageSize && addr > 0) {		// are we supposed to assume address cant be 0? 
			return true;
		} else {
			handleExit(-2);
			Lib.assertNotReached("Failed to exit on illegal address");
		}
		return false;
	}

	/** The program being run by this process. */
	protected Coff coff;

	/** This process's page table. */
	protected TranslationEntry[] pageTable;
	/** The number of contiguous pages occupied by the program. */
	protected int numPages;

	/** The number of pages in the program's stack. */
	protected final int stackPages = 8;

	private int initialPC, initialSP;
	private int argc, argv;

	private static final int pageSize = Processor.pageSize;
	private static final char dbgProcess = 'a';

	/* Added variables */
	/** The total number of processes currently/have been running. */
	private static int processCount = 0;
	/** The lock for processCount. */
	private static Lock processCountLock = new Lock();

	private static int numProcessesAlive = 0;
	private static Lock numProcessesAliveLock = new Lock();

	protected HashMap<Integer,Integer> childrenExitStatuses = new HashMap<Integer,Integer>();
	protected HashSet<Integer> childrenAbnormallyExited = new HashSet<Integer>();
	protected HashSet<UserProcess> childrenProcesses = new HashSet<UserProcess>();
	protected UserProcess parentProcess = null;
	private boolean exitingAbnormally = false;
	/** This is the process's unique ID. */
	protected int processID;
	/** Holds the process's files, indexed by file descriptors. */
	protected OpenFile[] fileArray;
	/** The number of free file slots this process has in fileArray. */
	protected int numFreeFileDesc;

	/*3. parent hold  2 hashmaps matches  child's PID with exit status and PID with abnormal exit flag
3. numprocessesalive should be == 0, not 1 in handleExit() [OK]
3. handleExit(): have some sort of flag for abnormal exit ?   
3. handleExec(): check the arguments first before initalizing the user processes [just swap?] yis
	 */

}
