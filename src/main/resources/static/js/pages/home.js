$(document).ready(function () {
    $(".sidebar-collapse").sideNav(); //sidebar initialization
});
var eventbus = new EventBus("/eventbus");
var lastMovies = $("#last-movies-tbody");
var totalStat = $("#total-stats-tbody");
var topMovies = $("#top-movies-tbody");
var wishlist = $("#wishlist-tbody");
var totalViews = $("#total-views");
var totalNewViews = $("#total-new-movies");
var totalRuntime = $("#total-runtime");
var totalDifferentMovies = $("#total-different-movies");
var totalCinema = $("#total-cinema-visits");

eventbus.onopen = function () {
    var lang;
    eventbus.send("translations", getCookie("lang"), function (error, reply) {
        lang = reply.body;
        fillLastMovies(lang);
        fillTotalStat(lang);
        fillTopMovies(lang);
        fillWishlist(lang);
    });
};

function fillTopMovies(lang) {
    eventbus.send("database_get_top_movies", {}, function (error, reply) {
        topMovies.empty();
        var data = reply['body']['rows'];
        console.log(data);
        if (data.length > 0) {
            $("#times-seen").show();
            $.each(data, function (i) {
                var movie = data[i];
                topMovies.append(
                    $.parseHTML(
                        '<tr onclick="openMovie(' + movie['MovieId'] + ')" class="cursor">' +
                        '<td class="grey-text top-movies-home">' + movie['Count'] + '</td>' +
                        '<td class="content-key grey-text text-darken-1">' + movie['Title'] + '</td>' +
                        '</tr>'
                    )
                );
            })
        } else {
            topMovies.append(
                $.parseHTML(
                    '<span class="card-title center grey-text text-darken-1">No movies</span>'
                )
            );
        }
        $("#top-movies-header").collapsible('open', 0);
    });
}

function fillLastMovies(lang) {
    eventbus.send("database_get_last_views", {}, function (error, reply) {
        lastMovies.empty();
        var data = reply['body']['rows'];
        if (data.length > 0) {
            $.each(data, function (i) {
                var movie = data[i];
                lastMovies.append(
                    $.parseHTML(
                        '<tr onclick="openMovie(' + movie['MovieId'] + ')" class="cursor">' +
                            '<td class="grey-text top-movies-home">' + getShortDayOfWeek(lang, movie['week_day']) + '</td>' +
                            '<td>' +
                                '<span class="content-key grey-text text-darken-1">' + movie['Title'] + '</span><br>' +
                                '<span class="grey-text">' + getMonth(movie['Start'], lang) + '</span>' +
                            '</td>' +
                            '<td>' +
                                getWasCinema(movie['WasCinema']) +
                            '</td>' +
                        '</tr>'
                    )
                );
            })
        } else {
            lastMovies.append(
                $.parseHTML(
                    '<span class="card-title center grey-text text-darken-1">No views</span>'
                )
            );
        }
        $("#last-views-header").collapsible('open', 0);
    });
}

function getWasCinema(wasCinema) {
    if (wasCinema) {
        return '<i class="fa fa-ticket top-movies-home green-text" aria-hidden="true"></i>';
    } return '';
}

function getShortDayOfWeek(lang, dayIndex) {
    var weekdays = [
        lang['STATISTICS_MON'],
        lang['STATISTICS_TUE'],
        lang['STATISTICS_WED'],
        lang['STATISTICS_THU'],
        lang['STATISTICS_FRI'],
        lang['STATISTICS_SAT'],
        lang['STATISTICS_SUN']
    ];

    return weekdays[dayIndex];
}

function fillTotalStat(lang) {
    eventbus.send("database_get_total_movie_count", {}, function (error, reply) {
        var data = reply['body']['rows'];
        totalViews.append(data[0]['total_movies']);
        totalRuntime.append(minutesToString(data[0]['Runtime']));
    });

    eventbus.send("database_get_new_movie_count", {}, function (error, reply) {
        var data = reply['body']['rows'];
        totalNewViews.append(data[0]['new_movies']);
    });

    eventbus.send("database_get_total_cinema_count", {}, function (error, reply) {
        var data = reply['body']['rows'];
        totalCinema.append(data[0]['total_cinema']);
    });

    eventbus.send("database_get_distinct_movie_count", {}, function (error, reply) {
        var data = reply['body']['rows'];
        totalDifferentMovies.append(data[0]['unique_movies']);
    });
    $("#stats-header").collapsible('open', 0);
}

function fillWishlist(lang) {
    eventbus.send("database_get_home_wishlist", {}, function (error, reply) {
        wishlist.empty();
        var data = reply['body']['rows'];
        if (data.length > 0) {
            $.each(data, function (i) {
                var movie = data[i];
                wishlist.append(
                    $.parseHTML(
                        '<tr onclick="openMovie(' + movie['MovieId'] + ')" class="cursor">' +
                        '<td>' +
                        '<span class="content-key grey-text text-darken-1">' + movie['Title'] + '</span><br>' +
                        '<span class="grey-text">' +  movie['Year'] + '</span>' +
                        '</td>' +
                        '</tr>'
                    )
                );
            })
        } else {
            wishlist.append(
                $.parseHTML(
                    '<span class="card-title center grey-text text-darken-1">No movies</span>'
                )
            );
        }
        $("#wishlist-header").collapsible('open', 0);
    });
}

function openMovie(movieId) {
    location.href = 'movies/?id=' + movieId;
}