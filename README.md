# Game Rooms [![Build Status](https://travis-ci.org/juanmbellini/game_rooms.svg?branch=master)](https://travis-ci.org/juanmbellini/game_rooms)

Second part of the Advanced Techniques in Videogames course project.

## Getting started
These instructions will install the system in your local machine.

### Prerequisites

1. Install Maven:

    #### Mac OSX
    ```
    $ brew install maven
    ```
    #### Ubuntu
    ```
    $ sudo apt-get install maven
    ```
    
    #### Other OSes
    Check [https://maven.apache.org/install.html](https://maven.apache.org/install.html).

2. Clone or download source code:

	```
	$ git clone https://github.com/juanmbellini/game_rooms.git
	```
	or
	
	```
	$ wget https://github.com/juanmbellini/game_rooms/archive/master.zip
	```

### Build
1. Change working directory to project root:

	```
	$ cd game_rooms
	```

2. Install project modules:

	```
	$ mvn clean install
	```

3. Create jar files:

	```
	$ mvn clean package
	```

## Usage

The system can be run executing the built jar. Run the following command in the console:

```
$ java -jar <path-to-jar>
```
If nothing was changed in the ```pom.xml``` file, and the output directory is the default, the path to the jar will be ```<PROJECT-ROOT>/target/game-rooms-1.0-SNAPSHOT.jar```, so the way to run the system is:

```
$ java -jar <PROJECT-ROOT>/target/game-rooms-1.0-SNAPSHOT.jar
```
You can customize the execution of the system including some options. The following sections will explain them.

### Printing usage message

To print the usage message, include the ```-h``` or ```--help``` options. For example:

```
$ java -jar <PROJECT-ROOT>/target/game-rooms-1.0-SNAPSHOT.jar --help
```

### Selecting the http server hostname

To select the server hostname (i.e the binding address), include the ```-H``` or ```--host``` options.
You can use an IP or a name (for instance, ```localhost```).
For example:

```
$ java -jar <PROJECT-ROOT>/target/game-rooms-1.0-SNAPSHOT.jar -H 192.168.1.10
```
The default value is ```localhost```.

### Selecting the http server listening port

To select the server port, include the ```-p``` or ```--port``` options.
You can set it to ```0``` to let the operating system choose for you.
For example

```
$ java -jar <PROJECT-ROOT>/target/game-rooms-1.0-SNAPSHOT.jar -H 192.168.1.10 -p 8000
```
The default value is ```9000```.

## Author

* [Juan Marcos Bellini](https://github.com/juanmbellini)


 