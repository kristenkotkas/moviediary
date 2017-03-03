fallback.ready(['jQuery', 'SockJS', 'EventBus'], function () {
    var eventbus = new EventBus("/eventbus");
    eventbus.onopen = function () {
        $("#search").keyup(function (e) {
            if (e.keyCode == 13) {
                $("#search-result").empty();
                eventbus.send("api_get_search", $("#search").val(), function (error, reply) {
                    var data = reply.body.results;
                    console.log(data);
                    $.each(data, function (i) {
                        var movie = data[i];
                        $("#search-result").append(
                            $.parseHTML(
                                '<div class="">' +
                                '<div class="search-results">' +
                                movie.release_date.split('-')[0] + ' . . . . ' +
                                movie.original_title +
                                '</div>' +
                                '</div>')
                        );
                    });

                });
            }
        });
    };
});