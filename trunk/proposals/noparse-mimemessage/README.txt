This is an overhaul of how MimeMessages are handled. The goal is to avoid parsing the data, and simply pass a reference to the underlying data, only parsing and instantiating the MimeMessage when needed (by a Mailet for example).

This also removes the dependency on Town.

EnhancedMimeMessage and JamesMimeMessage are removed and rolled into the MimeMessageWrapper.


Here's what to do:
- Copy files into main tree.
- Delete the following files:
  - town.jar
  - org.apache.james.core.JamesMimeMessageInputStream.java
  - org.apache.james.core.JamesMimeMessage.java
  - org.apache.james.core.EnhancedMimeMessage.java
  - org.apache.james.mailrepository.FileMimeMessageInputStream.java
  - org.apache.james.mailrepository.TownMimeMessageInputStream.java
  - org.apache.james.mailrepository.TownSpoolRepository.java
  - org.apache.james.transport.mailets.TownAlias.java
  - org.apache.james.transport.mailets.TownListserv.java
  - org.apache.james.userrepository.UsersTownRepository.java

build like normal... you might want to tweak the james-server.xml to use alternate repositories (like the JDBC version), should you be ambitious.

