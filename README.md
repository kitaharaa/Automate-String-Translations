## For what?
The aim is to use built .jar to modify string.xml for different locales during a pre-commit hook in the Android project.
Implemented in test project: https://github.com/kitaharaa/PreCommitHook-Test

## How to use
1. Run `./gradlew build` to get .jar. 
2. Run `java -jar arch_name.jar` to launch in GitHub Action. 
