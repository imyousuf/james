/****** Object:  Table [dbo].[Message]    Script Date: 10/3/2000 7:13:05 AM ******/
CREATE TABLE [dbo].[Message] (
	[message_name] [varchar] (200) NOT NULL ,
	[repository_name] [varchar] (200) NOT NULL ,
	[message_state] [varchar] (30) NOT NULL ,
	[error_message] [varchar] (200) NULL ,
	[sender] [varchar] (100) NOT NULL ,
	[recipients] [text] NOT NULL ,
	[remote_host] [varchar] (100) NOT NULL ,
	[remote_addr] [varchar] (20) NOT NULL ,
	[message_body] [image] NOT NULL ,
	[last_updated] [datetime] NOT NULL 
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO

ALTER TABLE [dbo].[Message] WITH NOCHECK ADD 
	CONSTRAINT [PK_Message] PRIMARY KEY  NONCLUSTERED 
	(
		[message_name]
	)  ON [PRIMARY] 
GO

