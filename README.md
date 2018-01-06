# Moviediary

Tegemist on filmide vaatamisi haldava rakendusega. Kasutajal on võimalus lisada vaadatud filme, ning aeg millal ta seda vaatas. Hiljem saab vaadata nähtud filmide ajalugu ning erinevat statistikat vaatamiste kohta. 

Meeskonna liikmed:
- Kristen Kotkas
- Kristjan Hendrik Küngas
- Alar Leemet

Viited:
- [Testkeskkond](https://movies.kyngas.eu)  
- [Projektiplaan](https://github.com/kristenkotkas/moviediary/wiki/Projektiplaan)
- [Prototüüp](https://github.com/kristenkotkas/moviediary/wiki/Protot%C3%BC%C3%BCp)

Etapid:
- [Etapp 1](https://github.com/kristenkotkas/moviediary/wiki/Etapp-1)
- [Etapp 2](https://github.com/kristenkotkas/moviediary/wiki/Etapp-2)
- [Etapp 3](https://github.com/kristenkotkas/moviediary/wiki/Etapp-3)
- [Etapp 4](https://github.com/kristenkotkas/moviediary/wiki/Etapp-4)
- [Etapp 5](https://github.com/kristenkotkas/moviediary/wiki/Etapp-5)
- [Etapp 6](https://github.com/kristenkotkas/moviediary/wiki/Etapp-6)
- [Etapp 7](https://github.com/kristenkotkas/moviediary/wiki/Etapp-7)

Jooksutamine:
- Kasuta testkeskkonda: [Testkeskkond](https://movies.kyngas.eu)  
- Ainult filmisoovitaja kasutamiseks jooksuta projekti kaustas:  
  - ```npm install``` 
  - ```npm run prod```
  - ```gradlew run```
- Kogu Moviediary jooksutamine:
  - Loo endale uus andmebaas /misc/MovieDiary_database.sql järgi
  - Kopeeri /misc/server.json /src/main/resources/ kausta
  - Täida ära server.json vajalikud seaded (TMDB ja OMDB võtmed, andmebaasi seaded, Facebooki ja Google OAuth andmed)
  - Jooksuta projekti kaustas:
    - ```npm install``` 
    - ```npm run prod```
    - ```gradlew run```
