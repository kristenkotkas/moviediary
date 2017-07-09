import $ from 'jquery';
import 'materialize-css/dist/js/materialize.min';
import EventBus from 'vertx3-eventbus-client';
import {getCookie, getMonth, minutesToString} from '../custom/loader';

$('#navbar-home').addClass('navbar-text-active');
$(() => $('.sidebar-collapse').sideNav()); //sidebar initialization

const eventbus = new EventBus('/eventbus');
const lastMovies = $('#last-movies-tbody');
const totalStat = $('#total-stats-tbody');
const topMovies = $('#top-movies-tbody');
const wishlist = $('#wishlist-tbody');
const totalViews = $('#total-views');
const totalNewViews = $('#total-new-movies');
const totalRuntime = $('#total-runtime');
const totalDifferentMovies = $('#total-different-movies');
const totalCinema = $('#total-cinema-visits');
const averageRuntime = $('#average-runtime');

eventbus.onopen = () => {
  /*DatabaseService.getTopMoviesHome('username', function (result, error) {
   console.log(result);
   console.log(error);
   });*/
  let lang;
  eventbus.send('translations', getCookie('lang'), (error, reply) => {
    lang = reply.body;
    fillLastMovies(lang);
    fillTotalStat(lang);
    fillTopMovies(lang);
    //fillWishlist(lang);
  });
};

function fillTopMovies(lang) {
  eventbus.send('database_get_top_movies', {}, (error, reply) => {
    topMovies.empty();
    const data = reply['body']['rows'];
    if (data.length > 0) {
      $('#times-seen').show();
      $.each(data, i => {
        const movie = data[i];
        topMovies.append(
            $.parseHTML(
                '<tr onclick="openMovie(' + movie['MovieId'] + ')" class="cursor">' +
                '<td class="grey-text top-movies-home">' + movie['Count'] + '</td>' +
                '<td class="content-key grey-text text-darken-1">' + movie['Title'] + '</td>' +
                '</tr>'
            )
        );
      });
    } else {
      topMovies.append(
          $.parseHTML(
              '<span class="card-title center grey-text text-darken-1">' + lang['HOME_NO_VIEWS'] + '</span>'
          )
      );
    }
    $('#top-movies-header').collapsible('open', 0);
  });
}

function fillLastMovies(lang) {
  eventbus.send('database_get_last_views', {}, (error, reply) => {
    lastMovies.empty();
    const data = reply['body']['rows'];
    if (data.length > 0) {
      $.each(data, i => {
        const movie = data[i];
        console.log(movie);
        lastMovies.append(
            $.parseHTML(
                '<tr onclick="openMovie(' + movie['MovieId'] + ')" class="cursor">' +
                '<td class="grey-text top-movies-home">' + getShortDayOfWeek(lang, movie['week_day']) + '</td>' +
                '<td>' +
                '<span class="content-key grey-text text-darken-1">' + movie['Title'] + '</span><br>' +
                '<span class="grey-text">' + getMonth(movie['Start'], lang) + '</span>' +
                '</td>' +
                '<td>' +
                getWasCinema(movie['WasCinema']) +
                '</td>' +
                '</tr>'
            )
        );
      });
    } else {
      lastMovies.append(
          $.parseHTML(
              '<span class="card-title center grey-text text-darken-1">' + lang['HOME_NO_VIEWS'] + '</span>'
          )
      );
    }
    $('#last-views-header').collapsible('open', 0);
  });
}

function getWasCinema(wasCinema) {
  return wasCinema ? '<i class="fa fa-ticket top-movies-home green-text" aria-hidden="true"></i>' : '';
}

function getShortDayOfWeek(lang, dayIndex) {
  const weekdays = [
    lang['STATISTICS_MON'],
    lang['STATISTICS_TUE'],
    lang['STATISTICS_WED'],
    lang['STATISTICS_THU'],
    lang['STATISTICS_FRI'],
    lang['STATISTICS_SAT'],
    lang['STATISTICS_SUN']
  ];
  return weekdays[dayIndex];
}

function getSinglePlural(singularString, pluralString, count) {
  return count === 1 ? singularString : pluralString;
}

function fillTotalStat(lang) {
  eventbus.send('database_get_total_movie_count', {}, (error, reply) => {
    const data = reply['body']['rows'];
    totalViews.append(data[0]['total_movies'] + ' ' + getSinglePlural(lang['HISTORY_VIEW'],
            lang['HISTORY_VIEWS'], data[0]['total_movies']));
    totalRuntime.append(minutesToString(data[0]['Runtime'], lang));
    averageRuntime.append(minutesToString(data[0]['Runtime'] / data[0]['total_movies'], lang));
  });
  eventbus.send('database_get_new_movie_count', {}, (error, reply) => {
    const data = reply['body']['rows'];
    totalNewViews.append(data[0]['new_movies'] + ' ' + getSinglePlural(lang['HISTORY_VIEW'],
            lang['HISTORY_VIEWS'], data[0]['new_movies']));
  });
  eventbus.send('database_get_total_cinema_count', {}, (error, reply) => {
    const data = reply['body']['rows'];
    totalCinema.append(data[0]['total_cinema'] + ' ' + getSinglePlural(lang['HOME_VISIT'],
            lang['HOME_VISITS'], data[0]['total_cinema']));
  });
  eventbus.send('database_get_distinct_movie_count', {}, function (error, reply) {
    const data = reply['body']['rows'];
    totalDifferentMovies.append(data[0]['unique_movies'] + ' ' + getSinglePlural(lang['HOME_MOVIE'],
            lang['HOME_MOVIES'], data[0]['unique_movies']));
  });
  $('#stats-header').collapsible('open', 0);
}

function openMovie(movieId) {
  location.href = 'movies/?id=' + movieId;
}