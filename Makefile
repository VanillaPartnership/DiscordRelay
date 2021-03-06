NAME=discord-relay
REPO=vanilla-partnership
PWD=$(shell pwd)

build:
	docker run --rm \
		-v $(PWD):/home/gradle/project \
		-w /home/gradle/project \
		gradle:4.8.1-jdk8-alpine \
		gradle --no-daemon clean build shadowJar
	docker build -t $(REPO)/$(NAME):latest .

clean:
	rm -rf build/ out/
