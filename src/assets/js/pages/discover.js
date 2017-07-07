import $ from "jquery";
import "materialize-css/dist/js/materialize.min";
import EventBus from "vertx3-eventbus-client";
import {getCookie} from "../custom/loader";

$(() => {
  $('.tooltipped').tooltip({ //tooltips initialization
    delay: 150,
    position: 'top',
    html: true
  });
  $('.sidebar-collapse').sideNav(); //sidebar initialization
});

const eventbus = new EventBus('/eventbus');
const addInput = $('#discover-add');
const discoverTable = $('#discover-body');
const searchResult = $('#discover-search');
const recommendations = {};

eventbus.onopen = () => {
  let lang;
  eventbus.send('translations', getCookie('lang'), (error, reply) => {
    lang = reply.body;
    addInput.keyup(e => {
      if (e.keyCode === 13) {
        getMovies();
      }
    });
  });
};

function getMovies() {
  eventbus.send('api_get_search', addInput.val(), (error, reply) => {
    const search = reply.body['results'];
    console.log(search);
    searchResult.empty();
    $.each(search, i => {
      const movie = search[i];
      searchResult.append(
          $.parseHTML(
              '<tr>' +
              '<td>' + movie['title'] + '</td>' +
              '<td>' + getYear(movie['release_date']) + '</td>' +
              '<td>' +
              '<a class="btn z-depth-0 waves-effect" onclick="getDiscover(' + movie['id'] + ')">Add</a>' +
              '</td>' +
              '</tr>'
          )
      );
    });
  });
}

function getDiscover(movieId) {
  eventbus.send('api_get_recommendations', movieId, (error, reply) => {
    console.log(reply.body);
    const data = reply.body['results'];
    $.each(data, i => {
      const movie = data[i];
      updateRecommendations(movie);
    });
    updateTable();
  });
}

function updateRecommendations(movie) {
  if (recommendations[movie['id']] === null) {
    const movieData = {};
    movieData.count = 1;
    movieData.id = movie['id'];
    movieData.title = movie['title'];
    movieData.poster = movie['poster_path'];
    recommendations[movie['id']] = movieData;
  } else {
    const count = recommendations[movie['id']]['count'];
    recommendations[movie['id']]['count'] = count + 1;
  }
}

function updateTable() {
  let count = 0;
  discoverTable.empty();
  const json = eval(recommendations); //fixme
  Object.keys(json).sort((a, b) => (json[b].count.toString()).localeCompare(json[a].count)).forEach(key => {
    if (count > 9) {
      return;
    }
    const movie = json[key];
    discoverTable.append(
        $.parseHTML(
            '<tr>' +
            '<td>' + movie['count'] + '</td>' +
            '<td>' + movie['id'] + '</td>' +
            '<td><img width="50" src="' + getPosterPath(movie['poster']) + '"></td>' +
            '<td>' + movie['title'] + '</td>' +
            '</tr>'
        )
    );
    count++;
  });
}

function getPosterPath(posterPath) {
  return posterPath === null ? '/static/img/nanPosterBig.jpg' : 'https://image.tmdb.org/t/p/w300' + posterPath;
}

function getYear(airDate) {
  return airDate === null ? '' : airDate.split('-')[0];
}
