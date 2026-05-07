.PHONY: build
.DEFAULT_GOAL := build-run
clean:
	./gradlew clean
build:
	./gradlew build
run:
	./gradlew appRun

build-run: build run
