import "jquery";
import "materialize-css/dist/js/materialize.min";
import EventBus from "vertx3-eventbus-client";
import Chart from "chart.js";
import {getCookie, getThisMonth, getThisWeek, makeDropList, minutesToString} from "../custom/loader";

$('#navbar-statistics').addClass('navbar-text-active');
$(() => {
  $('.datepicker').pickadate({ //calendar initialization
    //http://amsul.ca/pickadate.js/date/#options
    selectMonths: true,
    selectYears: 10,
    firstDay: 1
  });
  $('.tooltipped').tooltip({ //tooltips initialization
    delay: 150,
    position: 'top',
    html: true
  });
  $('.sidebar-collapse').sideNav(); //sidebar initialization
});

const eventbus = new EventBus('/eventbus');
const startDateField = $('#startingDay-stat');
const endDateField = $('#endDay-stat');
const yearDropdown = $('#stat-year-drop');
const daysChartCtx = $('#daysChart');
const daysChartSmallCtx = $('#daysChartSmall');
const yearsChartCtx = $('#yearsChart');
const yearsChartSmallCtx = $('#yearsChartSmall');
const timeChartCtx = $('#timeChart');
const timeChartSmallCtx = $('#timeChartSmall');
const monthChartCtx = $('#monthChart');
const monthChartSmallCtx = $('#monthChartSmall');
const collSearch = $('#search-coll-search');
const collFilters = $('#search-coll-filters');
const collQkSearch = $('#search-coll-qk-search');
const monthBack = $('#month-back-stat');
const monthNext = $('#month-next-stat');
const search = $('#search-stat');
const today = $('#today-stat');
const thisWeek = $('#this-week-stat');
const thisMonth = $('#this-month-stat');
const allTime = $('#all-time-stat');
const viewsTitle = $('#stat-view-title');
let monthIndex = 0;
const totalViews = $('#stat-total-views');
const totalRuntime = $('#stat-total-runtime');
const averageRuntime = $('#stat-average-runtime');
const topMovies = $('#stat-top-movies');
const monthDatasets = [];

daysChartCtx.attr('height', '250%');
daysChartSmallCtx.attr('height', '300%');
timeChartCtx.attr('height', '250%');
timeChartSmallCtx.attr('height', '500%');
yearsChartCtx.attr('height', '100%');
yearsChartSmallCtx.attr('height', '900%');
monthChartCtx.attr('height', '100%');
monthChartSmallCtx.attr('height', '900%');

const options = {
  scales: {
    yAxes: [{
      ticks: {
        beginAtZero: true
      },
      gridLines: {
        display: false
      }
    }],
    xAxes: [{
      gridLines: {
        display: false
      }
    }]
  }
};
const daysChart = new Chart(daysChartCtx, {
  type: 'bar',
  data: {
    labels: [],
    datasets: [
      {
        data: [],
        label: 'views',
        backgroundColor: [
          'rgb(56, 164, 221)',
          'rgb(139, 194, 73)',
          'rgb(254, 234, 57)',
          'rgb(241, 89, 43)',
          'rgb(248, 152, 29)',
          'rgb(205, 220, 55)',
          'rgb(243, 127, 128)'
        ]
      }
    ]
  },
  options: options
});
const daysChartSmall = new Chart(daysChartSmallCtx, {
  type: 'horizontalBar',
  data: {
    labels: [],
    datasets: [
      {
        data: [],
        label: 'views',
        backgroundColor: [
          'rgb(56, 164, 221)',
          'rgb(139, 194, 73)',
          'rgb(254, 234, 57)',
          'rgb(241, 89, 43)',
          'rgb(248, 152, 29)',
          'rgb(205, 220, 55)',
          'rgb(243, 127, 128)'
        ]
      }
    ]
  },
  options: options
});
const yearsChart = new Chart(yearsChartCtx, {
  type: 'bar',
  data: {
    labels: [],
    datasets: [
      {
        data: [],
        label: 'views',
        backgroundColor: 'rgb(56, 164, 221)'
      }
    ]
  },
  options: options
});
const yearsChartSmall = new Chart(yearsChartSmallCtx, {
  type: 'horizontalBar',
  data: {
    labels: [],
    datasets: [
      {
        data: [],
        label: 'views',
        backgroundColor: 'rgb(56, 164, 221)'
      }
    ]
  },
  options: options
});
const timeChart = new Chart(timeChartCtx, {
  type: 'bar',
  data: {
    labels: [
      '00', '01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11',
      '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23'
    ],
    datasets: [
      {
        data: [],
        label: 'views',
        backgroundColor: 'rgb(241, 89, 43)'
      }
    ]
  },
  options: options
});
const timeChartSmall = new Chart(timeChartSmallCtx, {
  type: 'horizontalBar',
  data: {
    labels: [
      '00', '01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11',
      '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23'
    ],
    datasets: [
      {
        data: [],
        label: 'views',
        backgroundColor: 'rgb(241, 89, 43)'
      }
    ]
  },
  options: options
});
const monthChart = new Chart(monthChartCtx, {
  type: 'bar',
  data: {
    labels: [],
    datasets: []
  },
  options: options
});
const monthChartSmall = new Chart(monthChartSmallCtx, {
  type: 'horizontalBar',
  data: {
    labels: [],
    datasets: []
  },
  options: options
});
const monthColors = [
  'rgb(234, 0, 38)',
  'rgb(0, 107, 170)',
  'rgb(86, 192, 201)',
  'rgb(172, 201, 54)',
  'rgb(0, 144, 64)',
  'rgb(220, 203, 56)',
  'rgb(247, 146, 44)',
  'rgb(214, 179, 111)',
  'rgb(168, 169, 127)',
  'rgb(157, 191, 193)',
  'rgb(0, 173, 226)',
  'rgb(233, 0, 119)'
];

eventbus.onopen = () => {
  let lang;
  eventbus.send('translations', getCookie('lang'), (error, reply) => {
    if ($(window).width() > 600) {
      openCollapsible();
    }
    lang = reply.body;
    console.log(lang);
    const labels = [
      lang['STATISTICS_MON'],
      lang['STATISTICS_TUE'],
      lang['STATISTICS_WED'],
      lang['STATISTICS_THU'],
      lang['STATISTICS_FRI'],
      lang['STATISTICS_SAT'],
      lang['STATISTICS_SUN']
    ];
    const monthLabels = [
      lang['STAT_JANUARY'],
      lang['STAT_FEBRUARY'],
      lang['STAT_MARCH'],
      lang['STAT_APRIL'],
      lang['STAT_MAY'],
      lang['STAT_JUNE'],
      lang['STAT_JULY'],
      lang['STAT_AUGUST'],
      lang['STAT_SEPTEMBER'],
      lang['STAT_OCTOBER'],
      lang['STAT_NOVEMBER'],
      lang['STAT_DECEMBER']
    ];
    fillDropDown(lang);
    search.keyup(e => {
      if (e.keyCode === 13) {
        search.click();
      }
    });
    today.keyup(e => {
      if (e.keyCode === 13) {
        today.click();
      }
    });
    thisWeek.keyup(e => {
      if (e.keyCode === 13) {
        thisWeek.click();
      }
    });
    thisMonth.keyup(e => {
      if (e.keyCode === 13) {
        thisMonth.click();
      }
    });
    allTime.keyup(e => {
      if (e.keyCode === 13) {
        allTime.click();
      }
    });
    search.click(() => makeHistory(eventbus, lang, startDateField.val(), endDateField.val(), monthIndex));
    today.click(() => {
      startDateField.pickadate('picker').set('select', new Date());
      endDateField.pickadate('picker').set('select', new Date());
      makeHistory(eventbus, lang, startDateField.val(), endDateField.val(), 0);
    });
    thisWeek.click(() => {
      const dates = getThisWeek();
      startDateField.pickadate('picker').set('select', dates['start']);
      endDateField.pickadate('picker').set('select', dates['end']);
      makeHistory(eventbus, lang, startDateField.val(), endDateField.val(), 0);
    });
    thisMonth.click(() => {
      const dates = getThisMonth(0);
      startDateField.pickadate('picker').set('select', dates['start']);
      endDateField.pickadate('picker').set('select', dates['end']);
      makeHistory(eventbus, lang, startDateField.val(), endDateField.val(), 0);
    });
    allTime.click(() => makeAllTime(eventbus, lang));
    monthBack.click(() => {
      const dates = getThisMonth(--monthIndex);
      startDateField.pickadate('picker').set('select', dates['start']);
      endDateField.pickadate('picker').set('select', dates['end']);
      makeHistory(eventbus, lang, startDateField.val(), endDateField.val(), monthIndex);
    });
    monthNext.click(() => {
      const dates = getThisMonth(++monthIndex);
      startDateField.pickadate('picker').set('select', dates['start']);
      endDateField.pickadate('picker').set('select', dates['end']);
      makeHistory(eventbus, lang, startDateField.val(), endDateField.val(), monthIndex);
    });
    daysChart['data']['labels'] = labels;
    daysChart.update();
    daysChartSmall['data']['labels'] = labels;
    daysChartSmall.update();
    monthChart['data']['labels'] = monthLabels;
    monthChart.update();
    monthChartSmall['data']['labels'] = monthLabels;
    monthChartSmall.update();
    allTime.click();
  });
};

function openCollapsible() {
  collFilters.collapsible('open', 0);
  collSearch.collapsible('open', 0);
  collQkSearch.collapsible('open', 0);
}

function fillDropDown(lang) {
  eventbus.send('database_get_all_time_meta', {
    'is-first': $('#seenFirst-stat').is(':checked'),
    'is-cinema': $('#wasCinema-stat').is(':checked')
  }, (error, reply) => {
    const data = reply.body['rows'];
    const start = new Date(data[0]['Start']).getFullYear();
    makeDropList(start, lang, 'stat', startDateField, endDateField);
  });
}

function makeHistory(eventbus, lang, start, end, monthInd) {
  monthIndex = monthInd;
  eventbus.send('database_get_history_meta', {
    'is-first': $('#seenFirst-stat').is(':checked'),
    'is-cinema': $('#wasCinema-stat').is(':checked'),
    'start': start,
    'end': end
  }, (error, reply) => {
    const data = reply.body['rows'];
    addTableHead(data, lang);
    getData(eventbus, lang, start, end);
  });
}

function makeAllTime(eventbus, lang) {
  eventbus.send('databxase_get_all_time_meta', {
    'is-first': $('#seenFirst-stat').is(':checked'),
    'is-cinema': $('#wasCinema-stat').is(':checked')
  }, (error, reply) => {
    const data = reply.body['rows'];
    let start = data[0]['Start'] === null ? new Date() : new Date(data[0]['Start']);
    const now = new Date();
    monthIndex = -1 * ((now.getFullYear() - start.getFullYear()) * 12 + (now.getMonth() - start.getMonth()));
    startDateField.pickadate('picker').set('select', start);
    endDateField.pickadate('picker').set('select', now);
    addTableHead(data, lang);
    getData(eventbus, lang, startDateField.val(), endDateField.val());
  });
}

function getData(eventbus, lang, start, end) {
  const seenFirstStat = $('#seenFirst-stat');
  const wasCinemaStat = $('#wasCinema-stat');

  eventbus.send('database_get_years_dist', {
    'is-first': seenFirstStat.is(':checked'),
    'is-cinema': wasCinemaStat.is(':checked'),
    'start': start,
    'end': end
  }, (error, reply) => {
    //console.log(reply);
    const data = reply.body['rows'];
    if (data.length > 0) {
      makeYearsChart(data);
    } else {
      makeEmptyYearsChart();
    }
  });
  eventbus.send('database_get_weekdays_dist', {
    'is-first': seenFirstStat.is(':checked'),
    'is-cinema': wasCinemaStat.is(':checked'),
    'start': start,
    'end': end
  }, (error, reply) => {
    const data = reply.body['rows'];
    if (data.length > 0) {
      makeDaysChart(data);
    } else {
      makeDaysChart(data);
    }
  });
  eventbus.send('database_get_time_dist', {
    'is-first': seenFirstStat.is(':checked'),
    'is-cinema': wasCinemaStat.is(':checked'),
    'start': start,
    'end': end
  }, (error, reply) => {
    const data = reply.body['rows'];
    if (data.length > 0) {
      makeTimeChart(data);
    } else {
      makeTimeChart(data);
    }
  });
  eventbus.send('database_get_month_year_distribution', {
    'is-first': seenFirstStat.is(':checked'),
    'is-cinema': wasCinemaStat.is(':checked'),
    'start': start,
    'end': end
  }, (error, reply) => {
    const data = reply.body;
    if (data.length > 0) {
      makeMonthChart(data);
    } else {
      makeMonthChart(data);
    }
  });
}

function makeMonthChart(data) {
  monthDatasets.length = 0;
  let index = 0;
  $.each(data, (key, value) => {
    const dataset = {};
    dataset['label'] = key.toString();
    dataset['backgroundColor'] = monthColors[index++ % 12];
    dataset['data'] = getMonthData(value);
    monthDatasets.push(dataset);
  });
  monthChart['data']['datasets'] = monthDatasets;
  monthChart.update();
  monthChartSmall['data']['datasets'] = monthDatasets;
  monthChartSmall.update();
}

function getMonthData(value) {
  const distr = {0: 0, 1: 0, 2: 0, 3: 0, 4: 0, 5: 0, 6: 0, 7: 0, 8: 0, 9: 0, 10: 0, 11: 0};
  $.each(value, i => {
    distr[i - 1] = value[i];
  });
  const distrArray = [];
  for (let i = 0; i < 12; i++) {
    distrArray.push(distr[i]);
  }
  return distrArray;
}

function makeDaysChart(data) {
  const distr = {0: 0, 1: 0, 2: 0, 3: 0, 4: 0, 5: 0, 6: 0};
  fillDaysData(data, distr);
  const distrArray = [];
  for (let i = 0; i < 7; i++) {
    distrArray.push(distr[i]);
  }
  makeDChart(daysChart, distrArray);
  makeDChart(daysChartSmall, distrArray);
}

function makeYearsChart(data) {
  const years = [];
  const distr = [];
  fillYearsData(data, years, distr);
  makeYChart(yearsChart, years, distr);
  makeYChart(yearsChartSmall, years, distr);
}

function makeEmptyYearsChart() {
  makeYChart(yearsChart, [], []);
  makeYChart(yearsChartSmall, [], []);
}

function makeTimeChart(data) {
  const distr = {
    0: 0, 1: 0, 2: 0, 3: 0, 4: 0, 5: 0, 6: 0, 7: 0, 8: 0, 9: 0, 10: 0, 11: 0, 12: 0,
    13: 0, 14: 0, 15: 0, 16: 0, 17: 0, 18: 0, 19: 0, 20: 0, 21: 0, 22: 0, 23: 0
  };
  const distrArray = [];
  fillTimeData(data, distr);
  for (let i = 0; i < 24; i++) {
    distrArray.push(distr[i]);
  }
  makeHChart(timeChart, distrArray);
  makeHChart(timeChartSmall, distrArray);
}

function fillDaysData(data, distr) {
  $.each(data, i => distr[data[i]['Day']] = data[i]['Count']);
}

function fillYearsData(data, years, distr) {
  const smallestYear = data[data.length - 1]['Year'];
  const greatestYear = data[0]['Year'];
  let i = 0;
  for (let year = greatestYear; year >= smallestYear; year--) {
    const yearData = data[i]['Year'];
    //console.log(yearData);
    if (yearData === year) {
      years.push(data[i]['Year']);
      distr.push(data[i]['Count']);
      i++;
    } else {
      years.push(year);
      distr.push(0);
    }
  }
}

function fillTimeData(data, distr) {
  $.each(data, i => distr[data[i]['Hour']] = data[i]['Count']);
}

function makeHChart(chart, distr) {
  chart['data']['datasets'][0]['data'] = distr;
  chart.update();
}

function makeDChart(chart, distr) {
  chart['data']['datasets'][0]['data'] = distr;
  chart.update();
}

function makeYChart(chart, years, distr) {
  chart['data']['labels'] = years;
  chart.update();
  chart['data']['datasets'][0]['data'] = distr;
  chart.update();
}

function addTableHead(data, lang) {
  fillTopMovies();
  if (data[0]['Count'] > 0) {
    viewsTitle.empty();
    totalViews.empty().append(data[0]['Count']);
    totalRuntime.empty().append(minutesToString(data[0]['Runtime'], lang));
    averageRuntime.empty().append(minutesToString(data[0]['Runtime'] / data[0]['Count'], lang));
  } else {
    addNotFound(lang);
    totalViews.empty().append('..');
    totalRuntime.empty().append('..');
    averageRuntime.empty().append('..');
  }
}

function fillTopMovies() {
  eventbus.send('database_get_top_movies_stat', {
    'is-first': $('#seenFirst-stat').is(':checked'),
    'is-cinema': $('#wasCinema-stat').is(':checked'),
    'start': startDateField.val(),
    'end': endDateField.val()
  }, (error, reply) => {
    topMovies.empty();
    const data = reply['body']['rows'];
    console.log(data);
    if (data.length > 0) {
      $.each(data, i => {
        const movie = data[i];
        topMovies.append(
            $.parseHTML(
                '<tr onclick="openMovie(' + movie['MovieId'] + ')" class="cursor">' +
                '<td class="grey-text">' + movie['Count'] + '</td>' +
                '<td class="content-key grey-text text-darken-1">' + movie['Title'] + '</td>' +
                '</tr>'
            )
        );
      });
    } else {
      topMovies.append(
          $.parseHTML(
              '<span class="card-title center grey-text text-darken-1">No movies</span>'
          )
      );
    }
  });
}

function addNotFound(lang) {
  viewsTitle.empty().append(
      '<div class="card z-depth-0">' +
      '<div class="card-title">' +
      '<span class="light grey-text text-lighten-1 not-found">' + lang['HISTORY_NOT_PRESENT'] + '</span><br>' +
      '</span>' +
      '</div>' +
      '</div>'
  );
}

function openMovie(movieId) {
  location.href = 'movies/?id=' + movieId;
}