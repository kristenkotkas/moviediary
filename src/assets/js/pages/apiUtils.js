import "jquery";

//'https://api.themoviedb.org/3/movie/{movie_id}?api_key=<<api_key>>&language=en-US'

const url = 'https://api.themoviedb.org';

const APIKEY_PREFIX1 = '&api_key='; // for search
const APIKEY_PREFIX2 = '?api_key='; // with specific id
const APPEND_TO_RESPONSE = '&append_to_response=';
const APIKEY = 'fbe0eec213cc4dd1dbb4a8c222273a3e';

const MOVIE_NAME = '/3/search/movie?query=';
const MOVIE_ID = '/3/movie/';
const RECOMMENDATIONS = '/recommendations';
const SIMILAR = '/similar';

const TV_NAME = '/3/search/tv?query=';
const TV_ID = '/3/tv/';

function getMovieSearch(movieName) {
  $.ajax({
    url: url + MOVIE_NAME + movieName.replace(' ', '-') + APIKEY_PREFIX1 + APIKEY,
    type: 'GET',
    contentType: 'json',
    success: data => data,
    error: e => console.log(e.message())
  });
}