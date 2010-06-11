
- needs eclipse and knopflerfish plugin to build
- needs knopflerfish cm_all-2.0.0.jar
- tested inside knopflerfish

Feel free to create build.xml and remove eclipse dependencies! :-)

Reads a modified config.xml and a description xml and maps this to flat, osgi-style properties

- supports ConfigurationAdmin factories.

Next steps:

- merging existing configuration instead of replacing. While running only changed configurations would be propagated.
- create some kind of plugin system for the description to allow custom tags/attributes for e.g. Mailets
- strictly check for required options and types
- cast String to Boolean and Integer


