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
        $('#year').empty().append(nullCheck(data.release_date).split('-')[0]);
        $('#release').empty().append(nullCheck(data.release_date));
        $('#runtime').empty().append(toNormalRuntime(nullCheck(data.runtime)));
        $('#language').empty().append(getStringFormArray(nullCheck(data.spoken_languages)));
        $('#genre').empty().append(getStringFormArray(nullCheck(data.genres)));
        $('#budget').empty().append(toNormalRevenue(nullCheck(data.budget)));
        $('#revenue').empty().append(toNormalRevenue(nullCheck(data.revenue)));
        $('#country').empty().append(getStringFormArray(nullCheck(data.production_countries)));
        $('#rating').empty().append(nullCheck(data.vote_average));
    });
};

var nullCheck = function (data) {
    if (data == 0 || data.length == 0) {
        return 'Unknown';
    } else return data;
};

var toNormalRuntime = function (runtime) {
    if (runtime == 'Unknown') {
        return 'Unknown';
    } else {
        var hour = ~~(runtime / 60);
        var min = runtime - 60 * hour;
        return hour + ' h ' + min + ' min';
    }
};

var getStringFormArray = function (jsonArray) {
    if (jsonArray == 'Unknown') {
        return 'Unknown';
    } else {
        console.log(jsonArray);
        if (jsonArray.length === 0) {
            return 'Unknown';
        }
        var result = "";

        $.each(jsonArray, function (i) {
            if (jsonArray[i].name !== '') {
                result += jsonArray[i].name + ', ';
            }
        });

        return result.slice(0, -2);
    }
};

var toNormalRevenue = function (revenue) {
    if (revenue === 'Unknown') {
        return 'Unknown';
    } else return revenue.toLocaleString() + ' $';
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
                    function () {
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