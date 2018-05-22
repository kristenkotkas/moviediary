$("#navbar-home").addClass('navbar-text-active');
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
var averageRuntime = $("#average-runtime");
var todayHistoryTable = $('#today-history-tbody');
eventbus.onopen = function () {
    var lang;
    eventbus.send("translations", getCookie("lang"), function (error, reply) {
        lang = reply.body;
        fillLastMovies(lang);
        fillTotalStat(lang);
        fillTopMovies(lang);
        fillWishlist(lang);
        fillTodayInHistory(lang);
    });
};

function fillTopMovies(lang) {
    eventbus.send("database_get_top_movies", {}, function (error, reply) {
        topMovies.empty();
        var data = reply['body']['rows'];
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
                    '<span class="card-title center grey-text text-darken-1">' + lang['HOME_NO_VIEWS'] + '</span>'
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
                    '<span class="card-title center grey-text text-darken-1">' + lang['HOME_NO_VIEWS'] + '</span>'
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
    return [
        lang['STATISTICS_SUN'],
        lang['STATISTICS_MON'],
        lang['STATISTICS_TUE'],
        lang['STATISTICS_WED'],
        lang['STATISTICS_THU'],
        lang['STATISTICS_FRI'],
        lang['STATISTICS_SAT']
    ][dayIndex];
}

function getSingPlur(singularString, pluralString, count) {
    if (count === 1) {
        return singularString
    } else {
        return pluralString;
    }
}

function fillTotalStat(lang) {
    eventbus.send('database_get_home_statistics', {}, function (error, reply) {
        var data = reply['body'];
        totalViews.append(data['total_views'] + ' ' + getSingPlur(lang['HISTORY_VIEW'], lang['HISTORY_VIEWS'], data['total_views']));
        totalRuntime.append(minutesToString(data['total_runtime'], lang));
        averageRuntime.append(minutesToString(data['total_runtime'] / data['total_views'], lang));
        totalNewViews.append(data['first_view'] + ' ' + getSingPlur(lang['HISTORY_VIEW'], lang['HISTORY_VIEWS'], data['first_view']));
        totalCinema.append(data['total_cinema'] + ' ' + getSingPlur(lang['HOME_VISIT'], lang['HOME_VISITS'], data['total_cinema']));
        totalDifferentMovies.append(data['unique_movies'] + ' ' + getSingPlur(lang['HOME_MOVIE'], lang['HOME_MOVIES'], data['unique_movies']));
        $("#stats-header").collapsible('open', 0);
    });
}

function fillWishlist(lang) {
    eventbus.send("database_get_last_lists_home", {}, function (error, reply) {
        wishlist.empty();
        var data = reply['body'];
        if (data.length > 0) {
            $.each(data, function (i) {
                var movie = data[i];
                wishlist.append(
                    $.parseHTML(
                        '<tr onclick="openMovie(' + movie['MovieId'] + ')" class="cursor">' +
                        '<td>' +
                        '<span class="content-key grey-text text-darken-1">' + movie['Title'] + '</span><br>' +
                        '<span class="grey-text content-key">' +  yearNullCheck(movie['Year'], lang) + '</span><br>' +
                        '<i class="grey-text">' +  movie['ListName'] + '</i>' +
                        '</td>' +
                        '</tr>'
                    )
                );
            })
        } else {
            wishlist.append(
                $.parseHTML(
                    '<span class="card-title center grey-text text-darken-1">' + lang['HOME_NO_MOVIES'] + '</span>'
                )
            );
        }
        $("#wishlist-header").collapsible('open', 0);
    });
}

function fillTodayInHistory(lang) {
    eventbus.send('database_get_today_in_history', {}, function (error, reply) {
        var data = reply.body;
        todayHistoryTable.empty();
        if (data.length > 0) {
            $.each(data, function (i) {
                var movie = data[i];
                todayHistoryTable.append(
                    $.parseHTML(
                        '<tr onclick="openMovie(' + movie['MovieId'] + ')" class="cursor">' +
                        '<td>' +
                        '<span class="content-key grey-text text-darken-1">' + movie['Title'] + '</span><br>' +
                        '<span class="grey-text">' + movie['Year'] + '</span>' +
                        '</td>' +
                        '<td>' +
                        getWasCinema(movie['WasCinema']) +
                        '</td>' +
                        '</tr>'
                    )
                );
            })
        } else {
            todayHistoryTable.append(
                $.parseHTML(
                    '<span class="card-title center grey-text text-darken-1">' + lang['HOME_NO_MOVIES'] + '</span>'
                )
            );
        }
        $("#today-history-header").collapsible('open', 0);
    });
}

function openMovie(movieId) {
    location.href = 'movies/?id=' + movieId;
}