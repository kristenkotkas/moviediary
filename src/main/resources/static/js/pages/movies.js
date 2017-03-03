var searchMovie = function (eventbus, movieId) {
    console.log(movieId);
    eventbus.send("api_get_movie", movieId.toString(), function (error, reply) {
        var data = reply.body;
        console.log(data);
        $("#search-result").empty().hide();
        $("#movie-result").empty().append(
            $.parseHTML(
                JSON.stringify(data, null, 2)
            )
        ).show();
        $("#search").val(data.title);
    });
};

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
                        var arrayOfNodes = $.parseHTML(
                            '<div>' +
                            '<a href="#" class="collection-item">' +
                            movie.release_date.split('-')[0] + ' . . . . ' +
                            movie.original_title +
                            '</a>' +
                            '</div>');
                        arrayOfNodes[0].firstChild.onclick = function () {
                            searchMovie(eventbus, movie.id);
                        };
                        $("#search-result").append(arrayOfNodes).show();
                        if (i === 9) {
                            return false;
                        }
                    });
                    $("#movie-result").hide();
                });
            }
        });
    };
});