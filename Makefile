# Makefile — build & run server/client (works for single-module or multi-module)
# Usage:
#   make run-server
#   make run-client
#   make package-server
#   make package-client
# You can override MAIN classes or mvn: e.g.
#   make SERVER_MAIN=com.example.chat.server.App run-server

MVN ?= mvn

# module directories (change if your layout differs)
SERVER_DIR := server
CLIENT_DIR := client

# default main classes (override on command line if needed)
SERVER_MAIN ?= com.example.chat.server.App
CLIENT_MAIN ?= com.example.chat.client.App

# helper: pick mvn -f when module pom exists
ifneq ($(wildcard $(SERVER_DIR)/pom.xml),)
SERVER_CMD := cd $(SERVER_DIR) && $(MVN)
SERVER_TARGET_DIR := $(SERVER_DIR)/target
else
SERVER_CMD := $(MVN)
SERVER_TARGET_DIR := target
endif

ifneq ($(wildcard $(CLIENT_DIR)/pom.xml),)
CLIENT_CMD := cd $(CLIENT_DIR) && $(MVN)
CLIENT_TARGET_DIR := $(CLIENT_DIR)/target
else
CLIENT_CMD := $(MVN)
CLIENT_TARGET_DIR := target
endif

.PHONY: help build run-server run-client package-server package-client \
        run-server-jar run-client-jar clean

help:
	@printf "Makefile targets:\n\n"
	@printf "  run-server       Build & run server using mvn exec:java\n"
	@printf "  run-client       Build & run client (tries javafx:run, falls back to exec:java)\n"
	@printf "  package-server   mvn clean package (server module or root)\n"
	@printf "  package-client   mvn clean package (client module or root)\n"
	@printf "  run-server-jar   Run the most recent server jar (java -jar)\n"
	@printf "  run-client-jar   Run the most recent client jar (java -jar)\n"
	@printf "  clean            mvn clean\n\n"
	@printf "Override vars: MVN, SERVER_MAIN, CLIENT_MAIN\nExample: make SERVER_MAIN=com.example.chat.Main run-server\n"

# compile only (both)
build:
	$(MVN) -q -DskipTests package

# run server (uses exec:java so dependencies classpath is provided)
run-server:
	@echo "Running server (main=$(SERVER_MAIN))..."
	@$(SERVER_CMD) -Dexec.mainClass="$(SERVER_MAIN)" exec:java

# run client: prefer javafx:run if available, otherwise exec:java
run-client:
	@echo "Running client (main=$(CLIENT_MAIN))..."
	@{ $(CLIENT_CMD) -q javafx:run || $(CLIENT_CMD) -Dexec.mainClass="$(CLIENT_MAIN)" exec:java; }

# package targets
package-server:
	@echo "Packaging server..."
	@$(SERVER_CMD) clean package

package-client:
	@echo "Packaging client..."
	@$(CLIENT_CMD) clean package

# run the most recently built jar (server)
run-server-jar:
	@echo "Running latest server jar from $(SERVER_TARGET_DIR)..."
	@sh -c 'jar=$$(ls -t $(SERVER_TARGET_DIR)/*.jar 2>/dev/null | head -n1) ; \
	 if [ -z "$$jar" ]; then echo "No jar found in $(SERVER_TARGET_DIR). Run make package-server first."; exit 1; fi ; \
	 echo "java -jar $$jar" ; java -jar "$$jar"'

# run the most recently built jar (client)
run-client-jar:
	@echo "Running latest client jar from $(CLIENT_TARGET_DIR)..."
	@sh -c 'jar=$$(ls -t $(CLIENT_TARGET_DIR)/*.jar 2>/dev/null | head -n1) ; \
	 if [ -z "$$jar" ]; then echo "No jar found in $(CLIENT_TARGET_DIR). Run make package-client first."; exit 1; fi ; \
	 echo "java -jar $$jar" ; java -jar "$$jar"'

clean:
	@$(MVN) clean

