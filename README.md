# moviediary

Kasutame:
- Frontend - MaterializeCss (CSS, JavaScript)- http://materializecss.com/
- Frontend - Handlebars (Java) - https://github.com/jknack/handlebars.java
- Backend - Vert.x (Java)- http://vertx.io/
- Andmebaas- MySQL
- Filmide andmed TMdb- https://www.themoviedb.org/?language=en

Igast documentatsiooni:
- Handlebars: https://jknack.github.io/handlebars.java/
- Vert.x: http://vertx.io/docs/, https://github.com/vert-x3/vertx-examples
- Materialize admin example: http://demo.geekslabs.com/materialize-v1.0/index.html

Käivitamine:
- (Tmdb api töötamiseks on vaja resources/server.json faili panna "tmdb_key": "<api_key>")
- Launcher main class
- Gradlest shadowJar task ja jooksuta /build/libs/moviediary.jar