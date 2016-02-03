Distributed-File-System
=======================

#Project
This project is to designed and implement a very simple distributed file system (DFS) in that a DFS client retrieves a file from a central DFS server and caches it in its /tmp directory for better performance. My implementation uses Java RMI.

#DFS Model
###(1)	One file cached in client’s disk and multiple files cached in server’s memory.
A DFS client program can cache just only one file in its local “/tmp” directory, whereas a DFS server can cache as many files as requested by DFS clients.
###(2)  Session semantics
A session in our system means a period of time while a file is being accessed or edited by a client’s emacs text editor. In other words, a session starts from file download from a server with the read or the write mode, allows the file to be opened for read only or for modification with emacs, and completes when the emacs closes the file. A single writer but no multiple writer clients are allowed, regardless of the number of reader clients. Specifically, a client is guaranteed to modify a file exclusively, but may read a file being modified by someone else.
###(3)  Delayed write
A client can maintain a file it has modified in its local “/tmp” directory until it needs to download a different file from a server or the server sends the client a write-back request as indicating that there is someone else who wants to modify the same file. In either case, the client must upload (i.e., write back) the modified file to the server. 
###(4)  Server-initiated invalidation
A server maintains a directory or a list of clients sharing the same file. If someone requests this file to download with the write mode, the server sends an invalidation message to all those clients that then invalidates their file copy. The server also sends a write-back message to the currently-modifying client that then uploads the latest file contents to the server.
