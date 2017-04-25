$(document).ready(function () {
    $(".sidebar-collapse").sideNav(); //sidebar initialization
});
var eventbus = new EventBus("/eventbus");
var lastMovies = $("#last-movies-tbody");
var totalStat = $("#total-stats-tbody");
var topMovies = $("#top-movies-tbody");
var wishlist = $("#wishlist-tbody");

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

function fillLastMovies(lang) {
    eventbus.send("database_get_last_views", {}, function (error, reply) {
        lastMovies.empty();
        var data = reply['body']['rows'];
        $.each(data, function (i) {
            var movie = data[i];
            lastMovies.append(
                $.parseHTML(
                    '<tr>' +
                        '<td class="content-key grey-text">' + movie['Title'] + '</td>' +
                        '<td class="grey-text">' + getMonth(movie['Start'], lang) + '</td>' +
                    '</tr>'
                )
            );
        })
    });
}

function fillTotalStat(lang) {

}

function fillTopMovies(lang) {
    eventbus.send("database_get_top_movies", {}, function (error, reply) {
        topMovies.empty();
        var data = reply['body']['rows'];
        console.log(data);
        $.each(data, function (i) {
            var movie = data[i];
            topMovies.append(
                $.parseHTML(
                    '<tr>' +
                    '<td class="content-key grey-text">' + movie['Title'] + '</td>' +
                    '<td class="grey-text">' + movie['Count'] + '</td>' +
                    '</tr>'
                )
            );
        })
    });
}

function fillWishlist(lang) {
    eventbus.send("database_get_home_wishlist", {}, function (error, reply) {
        wishlist.empty();
        var data = reply['body']['rows'];
        console.log(data);
        $.each(data, function (i) {
            var movie = data[i];
            wishlist.append(
                $.parseHTML(
                    '<tr>' +
                    '<td class="content-key grey-text">' + movie['Title'] + '</td>' +
                    '<td class="grey-text">' + movie['Year'] + '</td>' +
                    '</tr>'
                )
            );
        })
    });
}