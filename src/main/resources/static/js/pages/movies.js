var searchMovie = function (eventbus, movieId) {
    console.log(movieId);
    eventbus.send("api_get_movie", movieId.toString(), function (error, reply) {
        var data = reply.body;
        var posterPath = "";
        if (data.poster_path === null) {
            posterPath = '/static/img/nanPosterBig.jpg'
        } else {
            posterPath = 'https://image.tmdb.org/t/p/w500/' + data.poster_path;
        }
        console.log(data);
        $("#search-result").empty().hide();
        $("#movie-result").empty().append(
            $.parseHTML(
                JSON.stringify(data, null, 2)
            )
        ).show();
        //$("#search").val(data.original_title);
        $('#movie-title').text(data.original_title).addClass('movies-heading');
        $('#navbar-background').addClass('transparent');
        $("#movie-poster-card").empty().append(
            $.parseHTML(
                '<img src="' + posterPath + '" width="100%">'
            )
        );
        var backgroundPath = "";
        if (data.backdrop_path === null) {
            backgroundPath = "";
        } else {
            backgroundPath = 'https://image.tmdb.org/t/p/w1920' + data.backdrop_path;
        }
        $('#basic-info-box').removeClass('scale-out').addClass('scale-in');
        $('#seen-times').removeClass('scale-out').addClass('scale-in');
        $('#add-info-box').removeClass('scale-out').addClass('scale-in');
        $('#movie-poster-card').removeClass('scale-out').addClass('scale-in');

        $("#body").attr("background", backgroundPath);
        $('#year').empty().append(data.release_date.split('-')[0]);
        $('#release').empty().append(data.release_date);
        $('#language').empty().append(data.original_language);
        $('#genre').empty().append(data.genres);
        $('#budget').empty().append(data.budget);
        $('#revenue').empty().append(data.revenue);
        $('#country').empty().append(data.production_countries);
        $('#rating').empty().append(data.vote_average);
    });
};

var toNormalRuntime = function (runtime) {
    if (runtime === 0) {
        return 'Unknown'
    } else {
        var hour = ~~(runtime / 60);
        var min = runtime - 60 * hour;
        return hour + ' h ' + min + ' min';
    }
};

fallback.ready(['jQuery', 'SockJS', 'EventBus'], function () {
    var eventbus = new EventBus("/eventbus");
    eventbus.onopen = function () {
        $("#search").keyup(function (e) {
            if (e.keyCode == 13) {
                $("#search-result").empty();
                $('#basic-info-box').removeClass('scale-in').addClass('scale-out');
                $('#seen-times').removeClass('scale-in').addClass('scale-out');
                $('#add-info-box').removeClass('scale-in').addClass('scale-out');
                $('#movie-poster-card').removeClass('scale-in').addClass('scale-out');
                setTimeout(
                    function()
                    {
                        eventbus.send("api_get_search", $("#search").val(), function (error, reply) {
                            var data = reply.body.results;
                            console.log(data);
                            $.each(data, function (i) {
                                var movie = data[i];
                                var posterPath = "";
                                if (movie.poster_path !== null) {
                                    posterPath = 'https://image.tmdb.org/t/p/w92/' + movie.poster_path;
                                } else {
                                    posterPath = '/static/img/nanPosterSmall.jpg'
                                }
                                var arrayOfNodes = $.parseHTML(
                                    '<li class="collection-item search-object">' +
                                    '<div class="row">' +
                                    '<img src="' + posterPath + '" alt="" class="search-image" width="10%">' +
                                    '<span class="title search-object-text">' +
                                    movie.original_title +
                                    '</span>' +
                                    '</li>' +
                                    '</div>'
                                );
                                arrayOfNodes[0].onclick = function () {
                                    searchMovie(eventbus, movie.id);
                                };
                                $("#search-result").append(arrayOfNodes).show();
                                if (i === 9) {
                                    return false;
                                }
                            });
                            $("#movie-poster-card").empty();
                            //$("#movie-result").hide();
                        });
                    }, 205);

            }
        });
    };
});