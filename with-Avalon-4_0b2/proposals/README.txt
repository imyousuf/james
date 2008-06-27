A directory for proposals.

Place each proposal in a seperate subdirectory, with code in a subdirectory
 java etc.

Example:
proposals/<myproposal>/java/org/apache/james/....


To compile, use
./build.sh -Dproposal.dir="proposals/<myproposal>" -Dwith.proposal=true

