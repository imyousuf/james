How to read the picture:


-Each left-facing coloured rectangle represent a specific interface that 
 define the service provided by the Block. The red one is the SocketHandler etc.
-Bottom-facing yellow rectangle represent the common Block interface defining 
 how the framework interact with the Block.
-An arrow going from A to B means A is calling B.
-Dashed boxes are active (implements Runnable)

James is made of different blocks: the (SMTP, POP3, IMAP...) Servers.
The object Store and Logger can be shared with other servers or a private 
istance can be created.




