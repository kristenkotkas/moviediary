//'https://api.themoviedb.org/3/movie/{movie_id}?api_key=<<api_key>>&language=en-US'

var url = 'https://api.themoviedb.org';

var APIKEY_PREFIX1 = "&api_key="; // for search
var APIKEY_PREFIX2 = "?api_key="; // with specific id
var APPEND_TO_RESPONSE = "&append_to_response=";
var APIKEY = "fbe0eec213cc4dd1dbb4a8c222273a3e";

var MOVIE_NAME = "/3/search/movie?query=";
var MOVIE_ID = "/3/movie/";
var RECOMMENDATIONS = "/recommendations";
var SIMILAR = "/similar";

var TV_NAME = "/3/search/tv?query=";
var TV_ID = "/3/tv/";

function getMovieSearch(movieName) {
    $.ajax({
        url: url + MOVIE_NAME + movieName.replace(' ', '-') + APIKEY_PREFIX1 + APIKEY,
        type: 'GET',
        contentType: 'json',
        success: function (data) {
            return data;
        },
        error: function (e) {
            //console.log(e.message());
        }
    });
}
