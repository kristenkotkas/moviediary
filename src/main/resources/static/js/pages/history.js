fallback.ready(['jQuery', 'SockJS', 'EventBus'], function () {
    var eventbus = new EventBus("/eventbus");
    eventbus.onopen = function () {
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
                            '<th class="table-row">Title</th>' +
                            '<th>Date</th>' +
                            '<th>Time</th>' +
                            '</tr>');
                        Materialize.toast("Retrieved movies from database!", 5000);
                        $.each(data, function (i) {
                            $("#table").append(
                                $.parseHTML('<tr>' +
                                    '<td class="table-row">' + data[i].Title + '</td>' +
                                    '<td>' + data[i].Start + '</td>' +
                                    '<td>' + data[i].Time + '</td>' +
                                    '<td>' + data[i].DayOfWeek + '</td>' +
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
                            '<a class="light grey-text text-lighten-1 not-found">Views not present</a>' +
                            '</div>' +
                            '</div>'
                        );
                    }
                });
        });
    };
});