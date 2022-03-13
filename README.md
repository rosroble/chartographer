# Chartographer

Entry [task](https://github.com/gnkoshelev/chartographer) for [Kontur Internship](https://kontur.ru/education/programs/intern)

## Build & Run

- **mvn package** - run tests and build jar

Working directory should not contain any "{number}.bmp" and "{number}.json" files in order to run tests properly.

Make sure the "bmp_samples" folder is stored at the project dir.

- **java -jar chartographer-1.0.0.jar {*path-to-work-dir*}**

Run server on port 8080. *path-to-work-dir* is a path to the server storage. The directory must be created beforehand. If the given directory is not accessible, current directory is used.

Application creates "options.json" file to store unused id and load it at startup. Remove it if you need a clear setup.

