# Windows / GNU Make. Load JDK and proxy settings in the same process as Gradle.
# Release artifact: build/libs/ai_factory-1.0.0.jar (build includes tests and reobfJar).
SHELL := cmd.exe
.SHELLFLAGS := /C
.DEFAULT_GOAL := build

.PHONY: build package rebuild

# make / make build: build and package the release JAR.
build:
	powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$$ErrorActionPreference = 'Stop'; . .\Set-DevEnv.ps1; .\gradlew.bat build --console=plain; exit $$LASTEXITCODE"

# make package: alias for the normal release build.
package: build

# make rebuild: clean and build in one Gradle invocation (safe with make -j).
rebuild:
	powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$$ErrorActionPreference = 'Stop'; . .\Set-DevEnv.ps1; .\gradlew.bat clean build --console=plain; exit $$LASTEXITCODE"
