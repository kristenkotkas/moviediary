var searchMovie = function (eventbus, movieId, lang) {
    //console.log("LANG: " + JSON.stringify(lang));
    console.log(movieId);
    eventbus.send("api_get_movie", movieId.toString(), function (error, reply) {
        var data = reply.body;
        var posterPath = "";
        if (data.poster_path === null) {
            posterPath = '/static/img/nanPosterBig.jpg'
        } else {
            posterPath = 'https://image.tmdb.org/t/p/w500/' + data.poster_path;
        }

        document.title = data.title;
        console.log(data);
        $("#search-result").empty().hide();
        $('#movie-views-table').empty();
        $('#movie-title').text(data.original_title).addClass('movies-heading');
        $('#navbar-background').addClass('transparent');
        $("#movie-poster-card").empty().append(
            $.parseHTML(
                '<img src="' + posterPath + '" width="100%" alt="Poster">'
            )
        );

        var backgroundPath = "";
        if (data.backdrop_path === null) {
            backgroundPath = "";
        } else {
            backgroundPath = 'https://image.tmdb.org/t/p/w1920' + data.backdrop_path;
        }

        getMovieViews(eventbus, movieId, lang);

        $("#body").attr("background", backgroundPath);
        $('#year').empty().append(nullCheck(data.release_date, lang).split('-')[0]);
        $('#release').empty().append(getNormalDate(nullCheck(data.release_date, lang), lang));
        $('#runtime').empty().append(toNormalRuntime(nullCheck(data.runtime, lang), lang));

        $('#language').empty().append(getStringFormArray(nullCheck(data.spoken_languages, lang), lang));
        $('#genre').empty().append(getStringFormArray(nullCheck(data.genres, lang), lang));
        $('#budget').empty().append(toNormalRevenue(nullCheck(data.budget, lang), lang));
        $('#revenue').empty().append(toNormalRevenue(nullCheck(data.revenue, lang), lang));
        $('#country').empty().append(getStringFormArray(nullCheck(data.production_countries, lang), lang));
        $('#rating').empty().append(getRating(nullCheck(data.vote_average, lang), lang));

        $('#basic-info-box').removeClass('scale-out').addClass('scale-in');
        $('#seen-times').removeClass('scale-out').addClass('scale-in');
        $('#add-info-box').removeClass('scale-out').addClass('scale-in');
        $('#movie-poster-card').removeClass('scale-out').addClass('scale-in');
    });
};

var getMovieViews = function (eventbus, movieId, lang) {
    eventbus.send("database_get_movie_history", movieId.toString(), function (error, reply) {
        var data = reply.body['rows'];
        console.log(data);
        if (data.length > 0) {
            if (data.length > 1) {
                $('#seen-header').empty().append(lang.MOVIES_JS_SEEN_THIS + data.length + lang.MOVIES_JS_SEEN_TIMES);
            } else {
                $('#seen-header').empty().append(lang.MOVIES_JS_SEEN_ONCE);
            }
            $.each(data, function (i) {
                $('#movie-views-table').append(
                    $.parseHTML(
                        '<tr>' +
                        '<td class="content-key grey-text">' + getMonth(data[i].Start, lang) + '</td>' +
                        '<td class="grey-text"><i class=' + data[i].WasCinema + 'aria-hidden="true"></i></td>' +
                        '<tr>'
                    )
                );
            });
        } else {
            $('#seen-header').empty().append(lang.MOVIES_JS_NOT_SEEN);
        }
    });
};

var getNormalDate = function (date, lang) {
    var startArray = date.split('-');
    var dateFormat = new Date(date),
        locale = "en-us";
    var month = dateFormat.toLocaleString(locale,{month: "long"});
    return startArray[2] +  lang[month.toUpperCase()] + ' ' + startArray[0];
};

var nullCheck = function (data, lang) {
    if (data == 0 || data.length == 0) {
        return lang.MOVIES_JS_UNKNOWN;
    } else return data;
};

var getRating = function (data, lang) {
    if (data == lang.MOVIES_JS_UNKNOWN) {
        return lang.MOVIES_JS_UNKNOWN;
    } else {
        return data + ' / 10.0'
    }
};

var toNormalRuntime = function (runtime, lang) {
    if (runtime == lang.MOVIES_JS_UNKNOWN) {
        return lang.MOVIES_JS_UNKNOWN;
    } else {
        var hour = ~~(runtime / 60);
        var min = runtime - 60 * hour;
        return hour + ' h ' + min + ' min';
    }
};

var getStringFormArray = function (jsonArray, lang) {
    if (jsonArray == lang.MOVIES_JS_UNKNOWN) {
        return lang.MOVIES_JS_UNKNOWN;
    } else {
        //console.log(jsonArray);
        if (jsonArray.length === 0) {
            return lang.MOVIES_JS_UNKNOWN;
        }
        var result = "";

        $.each(jsonArray, function (i) {
            if (jsonArray[i].name !== '') {
                result += jsonArray[i].name + '<br>';
            }
        });

        return result.slice(0, -2);
    }
};

var toNormalRevenue = function (revenue, lang) {
    if (revenue === lang.MOVIES_JS_UNKNOWN) {
        return lang.MOVIES_JS_UNKNOWN;
    } else return revenue.toLocaleString() + ' $';
};

fallback.ready(['jQuery', 'SockJS', 'EventBus'], function () {
    var eventbus = new EventBus("/eventbus");
    eventbus.onopen = function () {

        var lang;
        eventbus.send("translations", getCookie("lang"), function (error, reply) {
            lang = reply.body;
            //console.log(lang);
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
                                console.log(data.length);
                                if (data.length > 0) {
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
                                            '</div>' +
                                            '</li>'
                                        );
                                        arrayOfNodes[0].onclick = function () {
                                            searchMovie(eventbus, movie.id, lang);
                                        };
                                        $("#search-result").append(arrayOfNodes).show();
                                        if (i === 9) {
                                            return false;
                                        }
                                    });
                                } else {
                                    $("#search-result").append(
                                        $.parseHTML(
                                            '<li class="collection-item">' +
                                            '<div class="row">' +
                                            '<h5>' + lang.MOVIES_JS_NO_MOVIES + '</h5>' +
                                            '</div>' +
                                            '</li>'
                                        )
                                    ).show();
                                }

                                $("#movie-poster-card").empty();
                            });
                        }, 405);

                }
            });
        });
    };
});