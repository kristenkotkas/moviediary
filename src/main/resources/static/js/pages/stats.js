$(document).ready(function () {
    $(".sidebar-collapse").sideNav(); //sidebar initialization
});

var eventbus = new EventBus("/eventbus");
eventbus.onopen = function () {
    var yearsChart = $('#yearsChart');
    var yearsChartSmall = $('#yearsChartSmall');
    var daysChart = $('#daysChart');
    var daysChartSmall = $('#daysChartSmall');
    var click_me = $('#click-me');

    click_me.click(function () {
        console.log('click');
        getData(eventbus);
    });

    function getData(eventbus) {
        eventbus.send("database_get_years_dist", {},
            function (error, reply) {
                console.log(reply);
                var data = reply.body['rows'];
                if (data.length > 0) {
                    makeYearsChart(data);
                }
            });

        eventbus.send("database_get_weekdays_dist", {},
            function (error, reply) {
                console.log(reply);
                var data = reply.body['rows'];
                if (data.length > 0) {
                    makeDaysChart(data);
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

        makeDChart(daysChart, 'bar', distrArray);
        makeDChart(daysChartSmall, 'horizontalBar', distrArray);

    }

    function makeYearsChart(data) {
        var years = [];
        var distr = [];

        fillYearsData(data, years, distr);
        makeYChart(yearsChart, 'bar', years, distr);
        makeYChart(yearsChartSmall, 'horizontalBar', years, distr);
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
