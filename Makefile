NAME=discord-relay
VERSION=$(shell git rev-parse HEAD)
REPO=vanilla-partnership
PWD=$(shell pwd)

build:
	docker run --rm \
		-v $(PWD):/home/gradle/project \
		-w /home/gradle/project \
		gradle:4.8.1-jdk8-alpine \
		gradle --no-daemon clean build jar
#	docker build -t $(REPO)/$(NAME):latest .

clean:
	rm -rf build/ out/
