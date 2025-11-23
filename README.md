# Groupspeak: Chat System Using Java

Simple client–server chat system built with Java and a JavaFX client.


## Prerequisites

* JDK 17+ (or the Java version you use)
* Maven
* make (GNU Make)

The repository includes a `Makefile` that provides convenient targets to build and run the server and client.


## Usage (Makefile)

Use the Makefile targets from the project root.

### Run

```bash
# Build and run the server (uses mvn exec:java)
make run-server

# Build and run the JavaFX client (tries javafx:run, falls back to exec:java)
make run-client
```

### Package

```bash
# Build/package server
make package-server

# Build/package client
make package-client
```

### Run packaged jars

```bash
# Run the most recently created server jar (from target/ or server/target)
make run-server-jar

# Run the most recently created client jar
make run-client-jar
```

### Clean

```bash
# Clean build artifacts (runs mvn clean)
make clean
```

### Override defaults

You can override the main classes or the `mvn` command on the fly:

```bash
# Run server using a different main class
make SERVER_MAIN=com.example.chat.CustomServer run-server

# Use a custom mvn (example: add -DskipTests)
make MVN='mvn -DskipTests' package-server
```

## Notes / Alternatives

If you prefer not to use `make`, here are the direct Maven one-liners equivalent to the Makefile targets:

```bash
# Run server directly with Maven (single command)
mvn -Dexec.mainClass="com.example.chat.server.App" exec:java

# Run client (JavaFX)
cd client && mvn clean javafx:run
```

