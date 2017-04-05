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
var yearDropdown = $("#stat-year-drop");
var daysChartCtx = $("#daysChart");
var daysChartSmallCtx = $("#daysChartSmall");
var yearsChartCtx = $("#yearsChart");
var yearsChartSmallCtx = $("#yearsChartSmall");
var timeChartCtx = $("#timeChart");
var timeChartSmallCtx = $("#timeChartSmall");
var options = {
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
var daysChart = new Chart(daysChartCtx, {
    type : 'bar',
    data: {
        labels: ['Mon.', 'Tue.', 'Wed.', 'Thu.', 'Fri.', 'Sat.', 'Sun.'],
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
var daysChartSmall = new Chart(daysChartSmallCtx, {
    type : 'horizontalBar',
    data: {
        labels: ['Mon.', 'Tue.', 'Wed.', 'Thu.', 'Fri.', 'Sat.', 'Sun.'],
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
var yearsChart = new Chart(yearsChartCtx, {
    type : 'bar',
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
var yearsChartSmall = new Chart(yearsChartSmallCtx, {
    type : 'horizontalBar',
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
var timeChart = new Chart(timeChartCtx, {
    type : 'bar',
    data: {
        labels: [0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23],
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
var timeChartSmall = new Chart(timeChartSmallCtx, {
    type : 'horizontalBar',
    data: {
        labels: [0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23],
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

eventbus.onopen = function () {
    var lang;
    eventbus.send("translations", getCookie("lang"), function (error, reply) {

        lang = reply.body;
        console.log(lang);
        var search = $("#search-stat");
        var today = $("#today-stat");
        var thisWeek = $("#this-week-stat");
        var thisMonth = $("#this-month-stat");
        var allTime = $("#all-time-stat");
        fillDropDown(lang);

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
        allTime.click(function () {
            makeAllTime(eventbus);
        });
    });
};

var fillDropDown = function (lang) {
    eventbus.send("database_get_all_time_meta",
        {
            'is-first': $("#seenFirst").is(':checked'),
            'is-cinema': $("#wasCinema").is(':checked')
        }
        , function (error, reply) {
            var data = reply.body['rows'];
            var start = new Date(data[0]['Start']).getFullYear();
            makeDropList(start, lang, 'stat', startDateField, endDateField);
        });
};

var makeHistory = function (eventbus, lang, start, end) {
    getData(eventbus, lang, start, end);
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
            //console.log(reply);
            var data = reply.body['rows'];
            if (data.length > 0) {
                makeYearsChart(data);
            } else {
                makeEmptyYearsChart()/*
                $('#year-chart-container').empty();
                $('#year-chart-small-container').empty();*/
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
            var data = reply.body['rows'];
            if (data.length > 0) {
                makeDaysChart(data);
            } else {
                makeDaysChart(data);/*
                $('#days-chart-container').empty();
                $('#days-chart-small-container').empty();*/
            }
        });

    eventbus.send("database_get_time_dist",
        {
            'is-first': $("#seenFirst-stat").is(':checked'),
            'is-cinema': $("#wasCinema-stat").is(':checked'),
            'start': start,
            'end': end
        }
        , function (error, reply) {
            var data = reply.body['rows'];
            if (data.length > 0) {
                makeTimeChart(data);
            } else {
                makeTimeChart(data);/*
             $('#year-chart-container').empty();
             $('#year-chart-small-container').empty();*/
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

    makeDChart(daysChart, distrArray);
    makeDChart(daysChartSmall, distrArray);

}

function makeYearsChart(data) {
    var years = [];
    var distr = [];
    fillYearsData(data, years, distr);
    makeYChart(yearsChart, years, distr);
    makeYChart(yearsChartSmall, years, distr);
}

function makeEmptyYearsChart() {
    makeYChart(yearsChart, [], []);
    makeYChart(yearsChartSmall, [], []);
}

function makeTimeChart(data) {
    var distr = {0:0, 1:0, 2:0, 3:0, 4:0, 5:0, 6:0, 7:0, 8:0, 9:0, 10:0, 11:0, 12:0,
        13:0, 14:0, 15:0, 16:0, 17:0, 18:0, 19:0, 20:0, 21:0, 22:0, 23:0};
    var distrArray = [];
    fillTimeData(data, distr);
    for (var i = 0; i < 24; i++) {
        distrArray.push(distr[i]);
    }
    makeHChart(timeChart, distrArray);
    makeHChart(timeChartSmall, distrArray);
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

function fillTimeData(data, distr) {
    $.each(data, function (i) {
        distr[data[i]['Hour']] = data[i]['Count'];
    });
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