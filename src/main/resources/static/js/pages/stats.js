$(document).ready(function () {
    $(".datepicker").pickadate({ //calendar initialization
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
    $(".sidebar-collapse").sideNav(); //sidebar initialization
});

var eventbus = new EventBus("/eventbus");
var startDateField = $("#startingDay-stat");
var endDateField = $("#endDay-stat");
eventbus.onopen = function () {
    var lang;
    eventbus.send("translations", getCookie("lang"), function (error, reply) {
        lang = reply.body;
        console.log(lang);

        var search = $("#search-stat");
        var today = $("#today-stat");
        var thisWeek = $("#this-week-stat");
        var thisMonth = $("#this-month-stat");
        var thisYear = $("#this-year-stat");
        var allTime = $("#all-time-stat");

        search.keyup(function (e) {
            if (e.keyCode === 13) {
                search.click();
            }
        });
        today.keyup(function (e) {
            if (e.keyCode === 13) {
                today.click();
            }
        });
        thisWeek.keyup(function (e) {
            if (e.keyCode === 13) {
                thisWeek.click();
            }
        });
        thisMonth.keyup(function (e) {
            if (e.keyCode === 13) {
                thisMonth.click();
            }
        });
        thisYear.keyup(function (e) {
            if (e.keyCode === 13) {
                thisYear.click();
            }
        });
        allTime.keyup(function (e) {
            if (e.keyCode === 13) {
                allTime.click();
            }
        });
        search.click(function () {
            makeHistory(eventbus, lang, startDateField.val(), endDateField.val());
        });
        today.click(function () {
            startDateField.pickadate('picker').set('select', new Date());
            endDateField.pickadate('picker').set('select', new Date());
            makeHistory(eventbus, lang, startDateField.val(), endDateField.val());
        });
        thisWeek.click(function () {
            var dates = getThisWeek();
            startDateField.pickadate('picker').set('select', dates['start']);
            endDateField.pickadate('picker').set('select', dates['end']);
            makeHistory(eventbus, lang, startDateField.val(), endDateField.val());
        });
        thisMonth.click(function () {
            var dates = getThisMonth();
            startDateField.pickadate('picker').set('select', dates['start']);
            endDateField.pickadate('picker').set('select', dates['end']);
            makeHistory(eventbus, lang, startDateField.val(), endDateField.val());
        });
        thisYear.click(function () {
            var dates = getThisYear();
            startDateField.pickadate('picker').set('select', dates['start']);
            endDateField.pickadate('picker').set('select', dates['end']);
            makeHistory(eventbus, lang, startDateField.val(), endDateField.val());
        });
        allTime.click(function () {
            makeAllTime(eventbus);
        });
    });

    var makeHistory = function (eventbus, lang, start, end) {
        eventbus.send("database_get_history_meta",
            {
                'is-first': $("#seenFirst-stat").is(':checked'),
                'is-cinema': $("#wasCinema-stat").is(':checked'),
                'start': start,
                'end': end
            }, function (error, reply) {
                var data = reply.body['rows'];
                getData(eventbus, lang, start, end);
            });
    };

    var makeAllTime = function (eventbus, lang) {
        eventbus.send("database_get_all_time_meta",
            {
                'is-first': $("#seenFirst").is(':checked'),
                'is-cinema': $("#wasCinema").is(':checked')
            }, function (error, reply) {
                var data = reply.body['rows'];
                startDateField.pickadate('picker').set('select', new Date(data[0]['Start']));
                endDateField.pickadate('picker').set('select', new Date());
                getData(eventbus, lang, startDateField.val(), endDateField.val());
            });
    };

    function getData(eventbus, lang, start, end) {
        eventbus.send("database_get_years_dist",
            {
                'is-first': $("#seenFirst-stat").is(':checked'),
                'is-cinema': $("#wasCinema-stat").is(':checked'),
                'start': start,
                'end': end
            }
            , function (error, reply) {
                console.log(reply);
                var data = reply.body['rows'];
                if (data.length > 0) {
                    makeYearsChart(data);
                } else {
                    $('#year-chart-container').empty();
                    $('#year-chart-small-container').empty();
                }
            });

        eventbus.send("database_get_weekdays_dist",
            {
                'is-first': $("#seenFirst-stat").is(':checked'),
                'is-cinema': $("#wasCinema-stat").is(':checked'),
                'start': start,
                'end': end
            }
            , function (error, reply) {
                console.log(reply);
                var data = reply.body['rows'];
                if (data.length > 0) {
                    makeDaysChart(data);
                } else {
                    $('#days-chart-container').empty();
                    $('#days-chart-small-container').empty();
                }
            });
    }

    function makeDaysChart(data) {
        var distr = {0: 0, 1: 0, 2: 0, 3: 0, 4: 0, 5: 0, 6: 0};

        fillDaysData(data, distr);

        var distrArray = [];

        for (var i = 0; i < 7; i++) {
            distrArray.push(distr[i]);
        }

        $('#days-chart-container').empty().append($.parseHTML(
            '<canvas class="hide-on-small-only" id="daysChart" height="250%"></canvas>'
        ));

        $('#days-chart-small-container').empty().append($.parseHTML(
            '<canvas class="hide-on-med-and-up" id="daysChartSmall" height="250%"></canvas>'
        ));

        makeDChart($('#daysChart'), 'bar', distrArray);
        makeDChart($('#daysChartSmall'), 'horizontalBar', distrArray);

    }

    function makeYearsChart(data) {
        var years = [];
        var distr = [];

        fillYearsData(data, years, distr);

        $('#year-chart-container').empty().append($.parseHTML(
            '<canvas class="hide-on-small-only" id="yearsChart" height="100%"></canvas>'
        ));

        $('#year-chart-small-container').empty().append($.parseHTML(
            '<canvas class="hide-on-med-and-up" id="yearsChartSmall" height="750%"></canvas>'
        ));

        makeYChart($('#yearsChart'), 'bar', years, distr);
        makeYChart($('#yearsChartSmall'), 'horizontalBar', years, distr);
    }

    function fillDaysData(data, distr) {
        $.each(data, function (i) {
            distr[data[i]['Day']] = data[i]['Count'];
        });
    }

    function fillYearsData(data, years, distr) {
        var smallestYear = data[data.length - 1]['Year'];
        var greatestYear = data[0]['Year'];

        var i = 0;
        for (var year = greatestYear; year >= smallestYear; year--) {
            var yearData = data[i]['Year'];
            //console.log(yearData);
            if (yearData == year) {
                years.push(data[i]['Year']);
                distr.push(data[i]['Count']);
                i++;
            } else {
                years.push(year);
                distr.push(0);
            }
        }
    }

    function makeDChart(chart, type, distr) {
        $('#week-card').show();

        new Chart(chart, {
            type: type,
            data: {
                labels: ['Mon.', 'Tue.', 'Wed', 'Thu.', 'Fri.', 'Sun.', 'Sat.'],
                datasets: [{
                    label: '# of Views',
                    data: distr,
                    backgroundColor: [
                        'rgb(56, 164, 221)',
                        'rgb(139, 194, 73)',
                        'rgb(254, 234, 57)',
                        'rgb(241, 89, 43)',
                        'rgb(248, 152, 29)',
                        'rgb(205, 220, 55)',
                        'rgb(243, 127, 128)'
                    ]
                }]
            },
            options: {
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
            }
        });

    }

    function makeYChart(chart, type, years, distr) {
        $('#year-card').show();

        new Chart(chart, {
            type: type,
            data: {
                labels: years,
                datasets: [{
                    label: '# of Years',
                    data: distr,
                    backgroundColor: 'rgb(54, 162, 235)'
                }]
            },
            options: {
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
            }
        });

    }
};
