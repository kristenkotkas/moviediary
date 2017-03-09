fallback.ready(['jQuery', 'SockJS', 'EventBus'], function () {
    var eventbus = new EventBus("/eventbus");
    eventbus.onopen = function () {
        var lang;
        eventbus.send("translations", getCookie("lang"), function (error, reply) {
            lang = reply.body;
            console.log(lang);
            $("#Search").click(function () {
                eventbus.send("database_get_history",
                    {
                        'is-first': $("#seenFirst").is(':checked'),
                        'is-cinema': $("#wasCinema").is(':checked'),
                        'start': $("#startingDay").pickadate('picker').get(),
                        'end': $("#endDay").pickadate('picker').get()
                    }, function (error, reply) {
                        console.log(reply);
                        var data = reply.body['rows'];
                        console.log(data.length);
                        if (data.length > 0) {
                            $("#viewsTitle").empty();
                            $("#table").empty().append(
                                '<tr>' +
                                '<th class="table-row">' + lang.HISTORY_TITLE + '</th>' +
                                '<th>' + lang.HISTORY_DATE + '</th>' +
                                '<th>' + lang.HISTORY_TIME + '</th>' +
                                '<th></th>' +
                                '<th></th>' +
                                '<th></th>' +
                                '</tr>');
                            $.each(data, function (i) {
                                $("#table").append(
                                    $.parseHTML('<tr>' +
                                        '<td class="table-row">' + data[i].Title + '</td>' +
                                        '<td>' + getMonth(data[i].Start, lang) + '</td>' +
                                        '<td>' + data[i].Time + '</td>' +
                                        '<td>' + lang[data[i].DayOfWeek] + '</td>' +
                                        '<td class="center"><i class=' + data[i].WasFirst + ' aria-hidden="true"></i></td>' +
                                        '<td class="center"><i class=' + data[i].WasCinema + ' aria-hidden="true"></i></td>' +
                                        '</tr>')
                                );
                            });
                        } else {
                            $("#table").empty();
                            $("#viewsTitle").empty().append(
                                '<div class="card z-depth-0">' +
                                '<div class="card-title">' +
                                '<a class="light grey-text text-lighten-1 not-found">' + lang.HISTORY_NOT_PRESENT + '</a>' +
                                '</div>' +
                                '</div>'
                            );
                        }
                    });
            });
        });

    };
});