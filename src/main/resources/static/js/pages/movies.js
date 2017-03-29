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
    $('.timepicker').pickatime({
        autoclose: true,
        twelvehour: false,
        default: 'now'
    });
    $('.modal').modal(); //movies modal initialization
});

var eventbus = new EventBus("/eventbus");
eventbus.onopen = function () {

    var lang;
    eventbus.send("translations", getCookie("lang"), function (error, reply) {
        lang = reply.body;
        enableParameterMovieLoading(eventbus, lang);

        $("#search").keyup(function (e) {
            if (e.keyCode === 13) {
                $("#search-button").click();
            }
        });

        $("#search-button").click(function () {
            $('#add-btn').off('click').off('keyup');
            $("#search-result").empty();
            $('#basic-info-box').removeClass('scale-in').addClass('scale-out');
            $('#seen-times').removeClass('scale-in').addClass('scale-out');
            $('#add-info-box').removeClass('scale-in').addClass('scale-out');
            $('#movie-poster-card').removeClass('scale-in').addClass('scale-out');
            $('#add-wishlist').removeClass('scale-in').addClass('scale-out');
            $('#plot').removeClass('scale-in').addClass('scale-out');
            $('#add-watch').removeClass('scale-in').addClass('scale-out');
            $('.collapsible').collapsible('close', 0);
            setTimeout(
                function () {
                    eventbus.send("api_get_search", $("#search").val(), function (error, reply) {
                        var data = reply.body['results'];
                        console.log(data.length);
                        if (data.length > 0) {
                            $.each(data, function (i) {
                                var movie = data[i];
                                var posterPath = "";
                                if (movie['poster_path'] !== null) {
                                    posterPath = 'https://image.tmdb.org/t/p/w92/' + movie['poster_path'];
                                } else {
                                    posterPath = '/static/img/nanPosterSmall.jpg'
                                }
                                var arrayOfNodes = $.parseHTML(
                                    '<li tabindex="' + (8 + i) + '" class="collection-item search-object">' +
                                    '<div class="row">' +
                                    '<img src="' + posterPath + '" alt="Poster for movie: '
                                    + movie['original_title'] + '" class="search-image" width="10%">' +
                                    '<span class="title search-object-text">' +
                                    movie['original_title'] +
                                    '</span>' +
                                    '</div>' +
                                    '</li>'
                                );
                                arrayOfNodes[0].onclick = function () {
                                    searchMovie(eventbus, movie.id, lang);
                                };

                                arrayOfNodes[0].addEventListener("keyup", function (e) {
                                    if (e.keyCode === 13) {
                                        searchMovie(eventbus, movie.id, lang);
                                    }
                                });

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
                                    '<h5>' + lang['MOVIES_JS_NO_MOVIES'] + '</h5>' +
                                    '</div>' +
                                    '</li>'
                                )
                            ).show();
                        }
                        $("#movie-poster-card").empty();
                    });
                }, 405);
        });
    });
};

var enableParameterMovieLoading = function (eventbus, lang) {
    var loadMovie = function (eventbus, lang) {
        var query = getUrlParam("id");
        if (query !== null && isNormalInteger(query)) {
            searchMovie(eventbus, query, lang);
        }
    };
    window.onpopstate = function () { //try to load movie on back/forward page movement
        loadMovie(eventbus, lang);
    };
    loadMovie(eventbus, lang); //load movie if url has param
};

var searchMovie = function (eventbus, movieId, lang) {
    //console.log("LANG: " + JSON.stringify(lang));
    console.log(movieId);
    eventbus.send("api_get_movie", movieId.toString(), function (error, reply) {
        var startDate = $("#watchStartDay");
        var startTime = $("#watchStartTime");
        var startNow = $("#watchStartNow");
        var startCalculate = $("#watchStartCalculate");

        var endDate = $("#watchEndDay");
        var endTime = $("#watchEndTime");
        var endNow = $("#watchEndNow");
        var endCalculate = $("#watchEndCalculate");

        var seenFirst = $("#watchSeenFirst");
        var wasCinema = $("#watchWasCinema");

        var commentFiled = $("#watchComment");

        var addToWatchBtn = $("#add-watch");
        var addButton = $('#add-btn');

        startNow.click(function (e) {
            startNowPress(startDate, startTime, e);
        });

        endNow.click(function (e) {
            endNowPress(endDate, endTime, e);
        });

        addToWatchBtn.click(function () {
            console.log('click');
            $('#modal1').modal('open');
            startDate.val('');
            startTime.val('');
            endDate.val('');
            endTime.val('');
            seenFirst.prop('checked', false);
            wasCinema.prop('checked', false);
            commentFiled.val('');
        });

        var data = reply.body;
        var posterPath = "";

        addButton.click(function () {
            var start = startDate.val() + ' ' + startTime.val();
            var end = endDate.val() + ' ' + endTime.val();

            if (start != ' ' && end != ' ' && commentFiled.val().length <= 500) {
                eventbus.send("database_insert_view", {
                    'movieId': movieId.toString(),
                    'start': start,
                    'end': end,
                    'wasFirst': seenFirst.is(':checked'),
                    'wasCinema': wasCinema.is(':checked'),
                    'comment': commentFiled.val()
                }, function (error, reply) {
                    Materialize.toast(data['original_title'] + ' added to views.', 2500);
                    console.log('hello');
                    console.log('reply ' + JSON.stringify(reply.body));
                    $('#modal1').modal('close');
                    getMovieViews(eventbus, movieId, lang);
                });
            }
        });

        endCalculate.click(function () {
            endCalcPress(endDate, endTime, startDate, startTime, data['runtime']);
        });

        startCalculate.click(function () {
            startCalcPress(endDate, endTime, startDate, startTime, data['runtime']);
        });

        if (data['runtime'] == 0) {
            startCalculate.prop("disabled", true);
            endCalculate.prop("disabled", true);
        } else {
            startCalculate.prop("disabled", false);
            endCalculate.prop("disabled", false);
        }

        if (data['poster_path'] === null) {
            posterPath = '/static/img/nanPosterBig.jpg'
        } else {
            posterPath = 'https://image.tmdb.org/t/p/w500/' + data['poster_path'];
        }

        document.title = lang['NAVBAR_MOVIES'] + ' - ' + data.title;
        console.log(data);

        $("#search-result").empty().hide();
        $('#movie-views-table').empty();
        $('#movie-title').text(data['original_title']).addClass('movies-heading');
        $('#navbar-background').addClass('transparent');
        $("#movie-poster-card").empty().append(
            $.parseHTML(
                '<img src="' + posterPath + '" width="100%" alt="Poster for movie: ' + data.title + '">'
            )
        );

        var backgroundPath = "";
        if (data['backdrop_path'] === null) {
            backgroundPath = "";
        } else {
            backgroundPath = 'https://image.tmdb.org/t/p/w1920' + data['backdrop_path'];
        }

        getMovieViews(eventbus, movieId, lang);

        $("#body").attr("background", backgroundPath);
        $('#year').empty().append(nullCheck(data['release_date'], lang).split('-')[0]);
        $('#release').empty().append(getNormalDate(nullCheck(data['release_date'], lang), lang));
        $('#runtime').empty().append(toNormalRuntime(nullCheck(data['runtime'], lang), lang));

        $('#language').empty().append(getStringFormArray(nullCheck(data['spoken_languages'], lang), lang));
        $('#genre').empty().append(getStringFormArray(nullCheck(data['genres'], lang), lang));
        $('#budget').empty().append(toNormalRevenue(nullCheck(data['budget'], lang), lang));
        $('#revenue').empty().append(toNormalRevenue(nullCheck(data['revenue'], lang), lang));
        $('#country').empty().append(getStringFormArray(nullCheck(data['production_countries'], lang), lang));
        $('#rating').empty().append(getRating(nullCheck(data['vote_average'], lang), lang));

        $('#basic-info-box').removeClass('scale-out').addClass('scale-in');
        $('#seen-times').removeClass('scale-out').addClass('scale-in');
        $('#add-info-box').removeClass('scale-out').addClass('scale-in');
        $('#movie-poster-card').removeClass('scale-out').addClass('scale-in');
        $('#add-watch').removeClass('scale-out').addClass('scale-in');
        $('#plot').removeClass('scale-out').addClass('scale-in');
        $('#add-wishlist').removeClass('scale-out').addClass('scale-in').off('click').off('keyup');

        inWishlist(eventbus, movieId, lang);

        $("#add-wishlist").click(function () {
            addToWishlist(eventbus, movieId, lang);
        });

        $("#add-wishlist").keyup(function (e) {
            if (e.keyCode === 13) {
                $("#add-wishlist").click();
            }
        });

        if (data['overview'].length > 0) {
            $('#plot-text').empty().append(data['overview']);
        }

        replaceUrlParameter("id", movieId);
    });
};

function addToWishlist(eventbus, movieId, lang) {
    eventbus.send("database_insert_wishlist", movieId);
    console.log(movieId + ' added to wishlist.');
    decorateInWishlist(lang);
}

function decorateInWishlist(lang) {
    $("#add-wishlist").removeClass('add-wishlist');
    $("#wishlist-text").empty().append(
        $.parseHTML(
            '<i class="fa fa-check left" aria-hidden="true"></i>' +
            lang['MOVIES_IN_WISHLIST']
        )
    ).removeClass('content-key');
}

function decorateNotInWIshlist(lang) {
    $("#add-wishlist").addClass('add-wishlist');
    $("#wishlist-text").empty().append(
        $.parseHTML(lang['MOVIES_ADD_WISHLIST'])
    ).addClass('content-key');
}

function inWishlist(eventbus, movieId, lang) {
    eventbus.send("database_get_in_wishlist", movieId, function (error, reply) {
        console.log('In wishlist: ' + reply.body['rows'].length);
        if (reply.body['rows'].length !== 0) {
            decorateInWishlist(lang);
        } else {
            decorateNotInWIshlist(lang);
        }
    });
}

var getMovieViews = function (eventbus, movieId, lang) {
    eventbus.send("database_get_movie_history", movieId.toString(), function (error, reply) {
        var data = reply.body['rows'];
        console.log(data);
        if (data.length > 0) {
            if (data.length > 1) {
                $('#seen-header').empty().append(lang['MOVIES_JS_SEEN_THIS'] + ' ' + data.length + lang['MOVIES_JS_SEEN_TIMES']);
            } else {
                $('#seen-header').empty().append(lang['MOVIES_JS_SEEN_ONCE']);
            }
            $('#movie-views-table').empty();
            $.each(data, function (i) {
                $('#movie-views-table').append(
                    $.parseHTML(
                        '<tr>' +
                        '<td class="content-key grey-text">' + getMonth(data[i]['Start'], lang) + '</td>' +
                        '<td class="grey-text"><i class="green-text ' + data[i]['WasCinema'] + '" aria-hidden="true"></i></td>' +
                        '</tr>'
                    )
                );
            });
        } else {
            $('#seen-header').empty().append(lang['MOVIES_JS_NOT_SEEN']);
        }
    });
};

var startNowPress = function (startDate, startTime, e) {
    var date = new Date();
    var time = getDualTime(date.getHours()) + ":" + getDualTime(date.getMinutes());
    e.stopPropagation();
    startDate.pickadate('picker').set('select', date);
    startTime.pickatime('show').pickatime('done');
    startTime.val(time);
};

var endNowPress = function (endDate, endTime, e) {
    var date = new Date();
    var time = getDualTime(date.getHours()) + ":" + getDualTime(date.getMinutes());
    e.stopPropagation();
    endDate.pickadate('picker').set('select', date);
    endTime.pickatime('show').pickatime('done');
    endTime.val(time);
};

var endCalcPress = function (endDate, endTime, startDate, startTime, movieLength) {
    var endingDate = new Date();

    if (startDate.val() != '' && startTime.val() != '') {
        var time = startTime.val().split(':');
        var pickDate = new Date(startDate.pickadate('picker').get('select')['pick']);
        var startingDate = new Date(pickDate.getFullYear(), pickDate.getMonth(), pickDate.getDate(), time[0], time[1], 0, 0);
        endingDate = plusMins(startingDate, movieLength);
    }

    endDate.pickadate('picker').set('select', endingDate);
    endTime.pickatime('show').pickatime('done');
    endTime.val(getDualTime(endingDate.getHours()) + ":" + getDualTime(endingDate.getMinutes()));
};

var startCalcPress = function (endDate, endTime, startDate, startTime, movieLength) {
    var endingDate = new Date();

    if (endDate.val() != '' && endTime.val() != '') {
        var time = endTime.val().split(':');
        var pickDate = new Date(endDate.pickadate('picker').get('select')['pick']);
        var startingDate = new Date(pickDate.getFullYear(), pickDate.getMonth(), pickDate.getDate(), time[0], time[1], 0, 0);
        endingDate = plusMins(startingDate, -1 * movieLength);
    }

    startDate.pickadate('picker').set('select', endingDate);
    startTime.pickatime('show').pickatime('done');
    startTime.val(getDualTime(endingDate.getHours()) + ":" + getDualTime(endingDate.getMinutes()));
};

function plusMins(date, minutes) {
    return new Date(date.getTime() + (minutes * 60000));
}

var getDualTime = function (digit) {
    digit = digit.toString();
    if (digit.length == 1) {
        return '0' + digit;
    }
    return digit;
};

var getNormalDate = function (date, lang) {
    if (date === lang['MOVIES_JS_UNKNOWN']) {
        return lang['MOVIES_JS_UNKNOWN'];
    } else {
        var startArray = date.split('-');
        var dateFormat = new Date(date),
            locale = "en-us";
        var month = dateFormat.toLocaleString(locale, {month: "long"});
        return startArray[2] + lang[month.toUpperCase()] + ' ' + startArray[0];
    }
};

var nullCheck = function (data, lang) {
    console.log(data);
    if (data === 0 || data.length === 0) {
        return lang['MOVIES_JS_UNKNOWN'];
    } else return data;
};

var getRating = function (data, lang) {
    if (data === lang['MOVIES_JS_UNKNOWN']) {
        return lang['MOVIES_JS_UNKNOWN'];
    } else {
        return data + ' / 10.0'
    }
};

var toNormalRuntime = function (runtime, lang) {
    if (runtime === lang['MOVIES_JS_UNKNOWN']) {
        return lang['MOVIES_JS_UNKNOWN'];
    } else {
        var hour = ~~(runtime / 60);
        var min = runtime - 60 * hour;
        return hour + ' h ' + min + ' min';
    }
};

var getStringFormArray = function (jsonArray, lang) {
    if (jsonArray === lang['MOVIES_JS_UNKNOWN']) {
        return lang['MOVIES_JS_UNKNOWN'];
    } else {
        //console.log(jsonArray);
        if (jsonArray.length === 0) {
            return lang['MOVIES_JS_UNKNOWN'];
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
    if (revenue === lang['MOVIES_JS_UNKNOWN']) {
        return lang['MOVIES_JS_UNKNOWN'];
    } else return revenue.toLocaleString() + ' $';
};


function replaceUrlParameter(param, value) {
    var url = location.href;
    var splitted = url.split('?');
    var shouldPushHistory = true;
    if (splitted.length > 1) {
        url = splitted[0];
        if (splitted[1].indexOf(value) !== -1) {
            shouldPushHistory = false;
        }
    }
    url = url + "?" + param + "=" + value;
    if (shouldPushHistory) {
        window.history.pushState('', '', url);
    }
}