DROP DATABASE James;

CREATE DATABASE James;

use James;

CREATE TABLE Message (
	message_name varchar (200) NOT NULL PRIMARY KEY,
	repository_name varchar (200) NOT NULL ,
	message_state varchar (30) NOT NULL ,
	error_message varchar (200) NULL ,
	sender varchar (100) NOT NULL ,
	recipients text NOT NULL ,
	remote_host varchar (100) NOT NULL ,
	remote_addr varchar (20) NOT NULL ,
	message_body longblob NOT NULL ,
	last_updated datetime NOT NULL 
);
