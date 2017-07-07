import "jquery";
import "materialize-css/dist/js/materialize.min";
import EventBus from "vertx3-eventbus-client";
import {getCookie, getUrlParam, isNormalInteger, replaceUrlParameter} from "../custom/loader";

$('#navbar-series').addClass('navbar-text-active');
$(() => {
  $('.tooltipped').tooltip({ //tooltips initialization
    delay: 150,
    position: 'top',
    html: true
  });
  $('.sidebar-collapse').sideNav(); //sidebar initialization
});
const eventbus = new EventBus('/eventbus');
const searchBar = $('#series-search');
const searchButton = $('#series-search-button');
const searchResultContainer = $('#tv-search-result-container');
const seenSeriesColl = $('#seen-series-coll');
const seenSeriesContainer = $('#seen-series-container');
const loaderContainer = $('#tv-loader');
const seriesTitle = $('#series-title');
const navbar = $('#navbar-background');
const body = $('#body');
const seriesDataContainer = $('#series-data-container');
let seenEpisodes;
const seenSeriesHeader = $('#seen-series-header');
let isMobile = false;

eventbus.onopen = () => {
  let lang;
  eventbus.send('translations', getCookie('lang'), (error, reply) => {
    if ($(window).width() <= 600) {
      isMobile = true;
    }
    lang = reply.body;
    enableParameterSeriesLoading(eventbus, lang);
    eventbus.send('database_get_watching_series', {}, (error, reply) => {
      const seenSeries = reply.body['rows'];
      //console.log('seenSeries', data);
      fillSeenSeries(seenSeries, lang);
      openSeenSeries();
      searchBar.keyup(e => {
        if (e.keyCode === 13) {
          searchButton.click();
        }
      });
      searchButton.click(() => getSeriesSearch(lang));
      seenSeriesHeader.click(() => eventbus.send('database_get_watching_series', {},
          (error, reply) => fillSeenSeries(reply.body['rows'], lang)));
    });
  });
};

function startLoading() {
  if (loaderContainer.empty()) {
    loaderContainer.append('<i class="fa fa-circle-o-notch white-text fa-spin fa-3x fa-fw"></i>');
  }
}

function endLoading() {
  loaderContainer.empty();
}

function enableParameterSeriesLoading(eventbus, lang) {
  const loadSeries = (eventbus, lang) => {
    const query = getUrlParam('id');
    if (query !== null && isNormalInteger(query)) {
      openSeries(parseInt(query), 1, lang);
    }
  };
  window.onpopstate = () => loadSeries(eventbus, lang); //try to load series on back/forward page movement
  loadSeries(eventbus, lang); //load series if url has param
}

function getSeriesSearch(lang) {
  eventbus.send('api_get_tv_search', searchBar.val(), (error, reply) => {
    seriesDataContainer.empty();
    if (isMobile) {
      closeSeenSeries();
    }
    const searchResult = reply.body['results'];
    searchResultContainer.empty();
    $.each(searchResult, i => {
      const searchedTvSeries = searchResult[i];
      const resultCardId = 'result_search' + searchedTvSeries['id'];
      //console.log(searchedTvSeries);
      searchResultContainer.append(
          $.parseHTML(
              '<div class="col s12 m12 l4">' +
              '<div class="card horizontal z-depth-0 search-object-series" id="' + resultCardId + '">' +
              '<div class="card-image">' +
              '<img src="' + getPosterPath(searchedTvSeries['poster_path']) + '" class="series-poster" ' +
              'alt="Poster for series: ' + searchedTvSeries['name'] + '">' +
              '</div>' +
              '<div class="card-stacked truncate">' +
              '<div class="card-content truncate">' +
              '<span class="truncate series-search-title">' + searchedTvSeries['name'] + '</span>' +
              '<span class="truncate">' +
              '<i class="fa fa-calendar-o" aria-hidden="true"></i>' +
              '<span class="series-date">' + getFirstAirDate(searchedTvSeries['first_air_date']) + '</span>' +
              '</span>' +
              '</div>' +
              '</div>' +
              '</div>' +
              '</div>'
          )
      );
      const resultCard = $(document.getElementById(resultCardId));
      resultCard.click(() => {
        console.log(resultCardId + ' clicked');
        openSeries(searchedTvSeries['id'], 1, lang);
        startLoading();
      });
    });
  });
}

function openSeries(seriesId, page, lang) {
  //seriesDataContainer.empty();
  //closeSeenSeries();
  eventbus.send('api_get_tv', {
    seriesId: seriesId,
    page: page
  }, (error, reply) => {
    if (isMobile) {
      closeSeenSeries();
    }
    searchResultContainer.empty();
    const seriesData = reply['body'];
    //console.log('seriesData', seriesData);
    changeDesign(seriesData);
    fillResultSeries(seriesData, page, lang);
    replaceUrlParameter('id', seriesId);
    endLoading();
  });
  //page = 1;
}

function fillResultSeries(seriesData, page, lang) {
  endLoading();
  eventbus.send('database_get_seen_episodes', seriesData['id'], (error, reply) => {
    seriesDataContainer.empty();
    addPagins(seriesData, page, lang, 'top');
    seenEpisodes = reply['body']['episodes'];
    for (let i = ((page - 1) * 10); i < page * 10; i++) {
      const season = 'season/' + i;
      if (seriesData[season] !== null) {
        const seasonData = seriesData[season];
        let episode = ' ';
        if (seasonData['episodes'].length > 1) {
          episode += lang['SERIES_EPISODE_SINGULAR'];
        } else {
          episode += lang['SERIES_EPISODE_PLURAL'];
        }
        seasonData['series_id'] = seriesData['id'];
        //console.log(seasonData);
        //console.log(seasonData);
        seriesDataContainer.append(
            $.parseHTML(
                '<li>' +
                '<div class="opacity-object collapsible-header collapsible-header-tv history-object grey-text">' +
                '<div class="row last-row">' +
                '<div class="col s3 m2 l1">' +
                '<img src="' + getPosterPath(seasonData['poster_path']) + '"' +
                ' alt="Poster for: ' + seasonData['name'] + '" class="img-100">' +
                '</div>' +
                '<div class="col s9 m10 l11">' +
                '<span class="tv-season-title grey-text text-darken-3">' + seasonData['name'] + '</span>' +
                '<span class="season-add-info grey-text text-darken-2">' +
                getYear(seasonData['air_date']) +
                seasonData['episodes'].length + episode +
                '</span><br>' +
                '<span class="description hide-on-med-and-down grey-text text-darken-4" >' +
                seasonData['overview'] + '</span>' +
                '</div>' +
                '</div>' +
                '</div>' +
                '<div class="collapsible-body collapsible-body-tv grey lighten-4">' +
                '<div class="row">' +
                '<div class="col s12 m12 l12">' +
                '<div class="row" id="whole-season-btn-container-' + seasonData['_id'] + '">' +
                '</div>' +
                '</div>' +
                '<div class="col s12 m12 l12" id="episode_container_' + i + '"></div>' +
                '</div>' +
                '</div>' +
                '</li>'
            )
        );
        if (seasonData['episodes'].length !== 0) {
          $(document.getElementById('whole-season-btn-container-' + seasonData['_id'])).append(
              $.parseHTML(
                  '<div class="col s12 m12 l6 xl3">' +
                  '<div class="card cursor green whole-season lighten-2 z-depth-0" ' +
                  'id="add-season-' + seasonData['_id'] + '">' +
                  '<div class="card-content">' +
                  '<div class="truncate white-text">' + lang['SERIES_ADD_SEASON'] + '</div>' +
                  '</div>' +
                  '</div>' +
                  '</div>' +
                  '<div class="col s12 m12 l6 xl3">' +
                  '<div class="card cursor red whole-season lighten-2 z-depth-0" ' +
                  'id="remove-season-' + seasonData['_id'] + '">' +
                  '<div class="card-content">' +
                  '<div class="truncate white-text">' + lang['SERIES_REMOVE_SEASON'] + '</div>' +
                  '</div>' +
                  '</div>' +
                  '</div>'
              )
          );
        } else {
          $(document.getElementById('whole-season-btn-container-' + seasonData['_id'])).append(
              $.parseHTML(
                  '<div class="col">' +
                  '<h5 class="truncate grey-text text-darken-2">' + lang['SERIES_NO_EPISODES'] + '</h5>' +
                  '</div>'
              )
          );
        }
        $(document.getElementById('add-season-' + seasonData['_id'])).click({
          seasonData: seasonData,
          lang: lang,
          container: $(document.getElementById('episode_container_' + i))
        }, addSeasonToWatch);
        $(document.getElementById('remove-season-' + seasonData['_id'])).click({
          seasonData: seasonData,
          lang: lang,
          container: $(document.getElementById('episode_container_' + i))
        }, removeSeasonFromWatch);
        getEpisodes(seasonData['episodes'], i, seriesData, lang);
      }
    }
    addPagins(seriesData, page, lang, 'bottom');
  });
}

function addPagins(seriesData, page, lang, type) {
  if (seriesData['number_of_seasons'] > 9) {
    const neededPages = Math.floor(seriesData['number_of_seasons'] / 10) + 1;
    seriesDataContainer.append(
        $.parseHTML(
            '<li class="z-depth-0">' +
            '<ul class="pagination center pagin-container" id="' + type + '-pagins-container">' +
            '</ul>' +
            '</li>'
        )
    );
    $.each(new Array(neededPages), pages => {
      const id = 'series-page_' + type + '_' + (pages + 1);
      $(document.getElementById(type + '-pagins-container')).append(
          $.parseHTML(
              '<li class="waves-effect pagin-button" id="' + id + '">' +
              '<a><span class="pagin-text">' + (pages + 1) + '</span></a>' +
              '</li>'
          )
      );
      $(document.getElementById(id)).click(() => {
        openSeries(seriesData['id'], pages + 1, lang);
        startLoading();
      });
      $(document.getElementById('series-page_' + type + '_' + page)).addClass('active');
    });
  }
}

function getEpisodes(episodes, seasonNumber, seriesData, lang) {
  const elem = $(document.getElementById('episode_container_' + seasonNumber));
  elem.empty();
  for (let episode = 1; episode <= episodes.length; episode++) {
    const id = 's' + seasonNumber + 'e' + episode;
    const episodeData = episodes[episode - 1];
    const seasonData = seriesData[('season/' + seasonNumber)];
    const ep = lang['SERIES_EPISODE_SHORT'] + ': ';
    const episodeCard = $.parseHTML(
        '<div class="col s12 m12 l6 xl3 grey-text">' +
        '<div class="card cursor episode-card" id="' + id + '">' +
        '<div class="card-content">' +
        '<div class="content-key truncate">' + episodeData['name'] + '</div>' +
        '<span class="badge" id="' + (id + '_check') + '"></span>' +
        '<div class="">' + ep + episodeData['episode_number'] + '</div>' +
        '<div class="smaller">' + getNormalDate(episodeData['air_date'], lang) + '</div>' +
        '</div>' +
        '</div>' +
        '</div>'
    );
    elem.append(episodeCard);
    $(episodeCard).data('episodeData', episodeData);
    if ($.inArray(episodeData['id'], seenEpisodes) !== -1) {
      changeToActive(
          $(document.getElementById(id)),
          $(document.getElementById(id + '_check')),
          episodeData);
      //console.log('inarray');
    }
    $(document.getElementById(id)).click({
      param: episodeData,
      id: id,
      seriesData: seriesData,
      seasonData: seasonData,
      lang: lang
    }, cardOnClick);
  }
}

function cardOnClick(event) {
  addEpisode(
      $(document.getElementById(event.data.id)),
      $(document.getElementById(event.data.id + '_check')),
      event.data.param,
      event.data.seriesData,
      event.data.seasonData,
      event.data.lang
  );
}

function addEpisode(card, element, data, seriesData, seasonData, lang) {
  //console.log(seriesData);
  if (element.children().length === 0) {
    addEpisodeToView(data, seriesData, seasonData, card, element, lang);
  } else {
    removeEpisode(card, element, data, lang);
  }
}

function changeToInActive(card, element, data) {
  card.removeAttr('style');
  card.removeClass('green').removeClass('white-text').addClass('grey-text');
  element.empty();
}

function addEpisodeToView(episodeData, seriesData, seasonData, card, element, lang) {
  const episodeId = episodeData['id'];
  const seriesId = seriesData['id'];
  console.log('seriesId', seriesId);
  console.log('episodeId', episodeId);
  eventbus.send('database_insert_episode', {
    'seriesId': seriesId,
    'episodeId': episodeId,
    'seasonId': seasonData['_id']
  }, (error, reply) => {
    //console.log('reply', reply);
    if (reply['body']['updated'] !== null) {
      console.log('episode added');
      changeToActive(card, element, episodeData);
      eventbus.send('database_get_watching_series', {}, (error, reply) => fillSeenSeries(reply.body['rows'], lang));
    }
  });
}

function removeEpisode(card, element, episodeData, lang) {
  const episodeId = episodeData['id'];
  eventbus.send('database_remove_episode', episodeId, (error, reply) => {
    //console.log('reply', reply);
    if (reply['body']['updated'] !== null) {
      console.log('episode removed');
      changeToInActive(card, element, episodeData);
      eventbus.send('database_get_watching_series', {}, (error, reply) => fillSeenSeries(reply.body['rows'], lang));
    }
  });
}

function addSeasonToWatch(event) {
  const data = event.data.seasonData;
  //console.log('seasonData', data);
  eventbus.send('database_insert_season_views', {
    seriesId: data['series_id'].toString(),
    seasonNr: data['season_number'].toString()
  }, (error, reply) => {
    if (reply['body']['updated'] !== null) {
      eventbus.send('database_get_watching_series', {}, (error, reply) => {
        fillSeenSeries(reply.body['rows'], event.data.lang);
        const episodesContainer = event.data.container;
        let timeout = 0;
        $.each(data['episodes'], i => setTimeout(() => {
          const card = episodesContainer[0]['childNodes'][i]['childNodes'];
          const episodeData = data['episodes'][i];
          const element = $(card).find('span.badge');
          /*console.log(card);
           console.log(episodeData);
           console.log('elemenr', element);*/
          changeToActive($(card), element, episodeData);
        }, timeout += 25));
      });
    }
  });
}

function removeSeasonFromWatch(event) {
  const data = event.data.seasonData;
  const seasonId = data['_id'];
  console.log(seasonId);
  eventbus.send('database_remove_season_views', seasonId, (error, reply) => {
    console.log(reply.body);
    if (reply['body']['updated'] !== null) {
      eventbus.send('database_get_watching_series', {}, (error, reply) => {
        fillSeenSeries(reply.body['rows'], event.data.lang);
        const episodesContainer = event.data.container;
        let timeout = 0;
        $.each(data['episodes'], i => setTimeout(() => {
          const card = episodesContainer[0]['childNodes'][i]['childNodes'];
          const element = $(card).find('span.badge');
          /*console.log(card);
           console.log(episodeData);
           console.log('elemenr', element);*/
          changeToInActive($(card), element);
        }, timeout += 25));
      });
    }
  });
}

function changeToActive(card, element, episodeData) {
  card.addClass('green').addClass('lighten-2').addClass('white-text');
  if (episodeData['still_path'] !== null) {
    const path = 'https://image.tmdb.org/t/p/w300' + episodeData['still_path'];
    card.css('background-image', 'url(' + path + ')').css('background-size', 'cover');
    card.addClass('white-text');
  }
  if (element.empty()) {
    element.append('<i class="fa fa-check fa-2x white-text" aria-hidden="true"></i>');
  }
}

function changeDesign(seriesData) {
  seriesTitle.text(seriesData['name']).addClass('movies-heading');
  //navbar.removeClass('cyan').addClass('transparent');
  body.attr('background', getBackdropPath(seriesData['backdrop_path']));
}

function fillSeenSeries(seriesData, lang) {
  seenSeriesContainer.empty();
  $.each(seriesData, i => {
    //console.log('series', seriesData[i]);
    const info = seriesData[i];
    const cardId = 'img_' + info['SeriesId'];
    const cardIdTitle = 'title_' + info['SeriesId'];
    const cardIdEpisodes = 'episodes_' + info['SeriesId'];
    const resultCardId = 'result_' + info['SeriesId'];
    seenSeriesContainer.append(
        $.parseHTML(
            '<div class="col s12 m12 l12">' +
            '<div class="card horizontal z-depth-0 search-object-series" id="' + resultCardId + '">' +
            '<div class="card-image">' +
            '<img id="' + cardId + '" class="series-poster" alt="Poster for series: ' + seriesData[i]['Title'] + '">' +
            '</div>' +
            '<div class="card-stacked truncate">' +
            '<div class="card-content">' +
            '<span class="truncate content-key" id="' + cardIdTitle + '"></span>' +
            //info['SeriesId'] +
            '</div>' +
            '<div class="card-action">' +
            '<span class="truncate" id="' + cardIdEpisodes + '"></span>' +
            '</div>' +
            '</div>' +
            '</div>' +
            '</div>'
        )
    );
    const imgCard = $(document.getElementById(cardId));
    const titleCard = $(document.getElementById(cardIdTitle));
    const episodesCard = $(document.getElementById(cardIdEpisodes));
    const resultCard = $(document.getElementById(resultCardId));
    resultCard.click(() => {
      openSeries(info['SeriesId'], 1, lang);
      startLoading();
    });
    decorateSeriesCard(imgCard, titleCard, episodesCard, info, lang);
  });
}

function getYear(airDate) {
  return airDate === null ? '' : (airDate.split('-')[0] + ' | ');
}

function getPosterPath(posterPath) {
  return posterPath === null ? '/static/img/nanPosterBig.jpg' : 'https://image.tmdb.org/t/p/w300' + posterPath;
}

function getBackdropPath(backdropPath) {
  return backdropPath === null ? '' : 'https://image.tmdb.org/t/p/w1920' + backdropPath;
}

function getFirstAirDate(firstAirDate) {
  return firstAirDate === '' ? ' ' : firstAirDate;
}

function openSeenSeries() {
  seenSeriesColl.collapsible('open', 0);
}

function closeSeenSeries() {
  seenSeriesColl.collapsible('close', 0);
}

function decorateSeriesCard(card, titleCard, episodeCard, info, lang) {
  if (info['Image'] !== '') {
    const path = 'https://image.tmdb.org/t/p/w300' + info['Image'];
    card.attr('src', path);
  } else {
    card.attr('src', '/static/img/nanPosterBig.jpg');
  }
  titleCard.append(info['Title']);
  let episodeSeen = info['Count'] > 1 ? lang['SERIES_EPISODE_SEEN_PLURAL'] : lang['SERIES_EPISODE_SEEN_SINGULAR'];
  episodeCard.append(
      $.parseHTML(
          '<span class="episodes-count">' + info['Count'] + '</span>' +
          '<span class="episodes-seen">' + episodeSeen + '</span>'
      )
  );
}

function getNormalDate(date, lang) {
  if (date === null) {
    return lang['MOVIES_JS_UNKNOWN'];
  }
  const startArray = date.split('-');
  const dateFormat = new Date(date);
  const locale = 'en-us';
  const month = dateFormat.toLocaleString(locale, {month: 'long'});
  const weekday = new Date(startArray[0], startArray[1] - 1, startArray[2]).getDay();
  //console.log(weekday); //0=Sun, 1=Mon, ..., 6=Sat
  return getShortDayOfWeek(lang, weekday) + ' ' + startArray[2] + lang[month.toUpperCase()] + ' ' + startArray[0];
}

function getShortDayOfWeek(lang, dayIndex) {
  const weekdays = [
    lang['STATISTICS_SUN'],
    lang['STATISTICS_MON'],
    lang['STATISTICS_TUE'],
    lang['STATISTICS_WED'],
    lang['STATISTICS_THU'],
    lang['STATISTICS_FRI'],
    lang['STATISTICS_SAT']
  ];
  return weekdays[dayIndex];
}