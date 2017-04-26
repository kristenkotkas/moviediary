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
        $.each(data, function (i) {
            var movie = data[i];
            topMovies.append(
                $.parseHTML(
                    '<tr onclick="openMovie(' + movie['MovieId'] + ')" class="cursor">' +
                        '<td class="grey-text center top-movies-home">' + movie['Count'] + '</td>' +
                        '<td class="content-key grey-text text-darken-1">' + movie['Title'] + '</td>' +
                    '</tr>'
                )
            );
        })
    });
}

function fillLastMovies(lang) {
    eventbus.send("database_get_last_views", {}, function (error, reply) {
        lastMovies.empty();
        var data = reply['body']['rows'];
        $.each(data, function (i) {
            var movie = data[i];
            lastMovies.append(
                $.parseHTML(
                    '<tr onclick="openMovie(' + movie['MovieId'] + ')" class="cursor">' +
                        '<td>' +
                            '<span class="content-key grey-text text-darken-1">' + movie['Title'] + '</span><br>' +
                            '<span class="grey-text">' + getMonth(movie['Start'], lang) + '</span>' +
                        '</td>' +
                    '</tr>'
                )
            );
        })
    });
}

function fillTotalStat(lang) {
    eventbus.send("database_get_total_movie_count", {}, function (error, reply) {
        var data = reply['body']['rows'];
        totalViews.append(data[0]['total_movies'] + ' views');
        totalRuntime.append(minutesToString(data[0]['Runtime']));
    });

    eventbus.send("database_get_new_movie_count", {}, function (error, reply) {
        var data = reply['body']['rows'];
        totalNewViews.append(data[0]['new_movies'] + ' views');
    });

    eventbus.send("database_get_distinct_movie_count", {}, function (error, reply) {
        var data = reply['body']['rows'];
        totalDifferentMovies.append(data[0]['unique_movies'] + ' movies');
    });
}

function fillWishlist(lang) {
    eventbus.send("database_get_home_wishlist", {}, function (error, reply) {
        wishlist.empty();
        var data = reply['body']['rows'];
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
    });
}

function openMovie(movieId) {
    location.href = 'movies/?id=' + movieId;
}