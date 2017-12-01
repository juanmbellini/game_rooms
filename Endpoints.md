# Endpoints

The following document describes how to communicate with the system through the HTTP Server.

The system exposes a REST API, using JSON as the Content Type.

## Game Rooms

### Model

A game room has the following fields:

| Field		| Description 	        	|
|:-----------|:-------------------------|
| name			| The game room name   		|
| capacity	| Max. amount of players 	|
| players		| Players in the game room	|
| locationUrl	| The URL of the game room	|



### List All Game Rooms

Returns an array of game rooms.

#### Request URL: 

**GET /game-rooms**

#### Request Headers: 

* **Accept: application/json**

#### Responses:

* **200 OK:** The request was successfully answered. Data is in the body of the response.
* **408 Request Timeout:** The request reached the timeout set for it.



### Get a specific Game Rooms by Name

Returns a given game room, specifying a name.

#### Request URL: 

**GET /game-rooms/:name**

#### Path params
* **name:** The name of the game room to be retrieved.

#### Request Headers: 

* **Accept: application/json**

#### Responses:

* **200 OK:** The request was successfully answered. Data is in the body of the response.
* **404 Not Found:** There is no game room with the given name.
* **408 Request Timeout:** The request reached the timeout set for it.



### Create a game room

Creates a new game room.

#### Request URL: 

**POST /game-rooms**

#### Request Headers: 

* **Content-Type: application/json**

#### Request Body: 

The request msut include a JSON, indicating the ```name``` and the ```capacity``` for the game room.
For example, you can include

```json
{
	"name":"Super Game Room",
	"capacity": 42
}
```

#### Responses:

* **200 OK:** The request was successfully answered. Data is in the body of the response.
* **409 Conflict:** There is another game room with the given name.
* **408 Request Timeout:** The request reached the timeout set for it.

#### Response Headers: 

* **Location:** Will include the url of the recently created game room



### Remove a game room

Removes a given game room, specifying a name.
**This is an idempotent request**.

#### Request URL: 

**DELETE /game-rooms/:name**

#### Path params
* **name:** The name of the game room to be removed.

#### Responses:

* **204 No Content:** The request was successfully processed.
* **408 Request Timeout:** The request reached the timeout set for it.



### Add a player to a game room

Adds a given player into a given game room, if it has space.
**This is an idempotent request** (won't affect if you add an already added player).

#### Request URL: 

**PUT /game-rooms/:name/players/:playerId**

#### Path params
* **name:** The name of the game room to which a player will be added.
* **playerId:** The id of the player to be added.

#### Responses:

* **204 No Content:** The request was successfully processed.
* **404 Not Found:** No Game Room with the specified name.
* **408 Request Timeout:** The request reached the timeout set for it.
* **409 Conflict:** The game room is full (the amount of players reached the max. capacity).



### Remove a player from a game room

Removes a given player into a given game room, if it has space.
**This is an idempotent request** (won't affect if you remove a non-added player).

#### Request URL: 

**DELETE /game-rooms/:name/players/:playerId**

#### Path params
* **name:** The name of the game room from which a player will be removed.
* **playerId:** The id of the player to be removed.

#### Responses:

* **204 No Content:** The request was successfully processed.
* **404 Not Found:** No Game Room with the specified name.
* **408 Request Timeout:** The request reached the timeout set for it.

