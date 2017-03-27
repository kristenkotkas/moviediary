var eventbus = new EventBus("/eventbus");
eventbus.onopen = function () {
    var chart = $('#myChart');
    var chartSmall = $('#myChartSmall');
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
                    var years = [];
                    var distr = [];

                    fillData(data, years, distr);
                    makeChart(chart, 'bar', years, distr);
                    makeChart(chartSmall, 'horizontalBar', years, distr);
                }
            });
    }

    function fillData(data, years, distr) {
        var smallestYear = data[data.length - 1]['Year'];

        var i = 0;
        for (var year = 2017; year >= smallestYear; year--) {
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

    function makeChart(chart, type, years, distr) {
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
