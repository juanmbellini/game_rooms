# Game Rooms

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

2. Install maven modules:

	```
	$ mvn clean install
	```

3. Create jar files:

	```
	$ mvn clean package
	```

## Author

* [Juan Marcos Bellini](https://github.com/juanmbellini)


 