$(document).ready(function () {
    $(".sidebar-collapse").sideNav(); //sidebar initialization
});

var eventbus = new EventBus("/eventbus");
eventbus.onopen = function () {
    var lang;
    eventbus.send("translations", getCookie("lang"), function (error, reply) {
        lang = reply.body;
        console.log(lang);
        eventbus.send("database_get_wishlist", {}, function (error, reply) {
            var data = reply.body['rows'];
            if (typeof Storage !== 'undefined') {
                localStorage.setItem("wishlist_data", JSON.stringify(data));
            }
            console.log("//////////////////arrrr");
            console.log(data);
            //addTableHead(lang);
            addTableData(data, lang);
        });
    });
};
eventbus.onclose = function (json) {
    if (json.wasClean === false) { //connection lost
        if (typeof Storage !== 'undefined') {
            var data = JSON.parse(localStorage.getItem("wishlist_data"));
            $("#wishlist-result").empty();
            addTableData(data);
        }
    }
};

function addTableData(data, lang) {
    var timeout = 0;
    $.each(data, function (i) {
        setTimeout(function () {
            var posterPath = "";
            var movie = data[i];
            if (movie['Image'] !== "") {
                posterPath = 'https://image.tmdb.org/t/p/w342' + movie['Image'];
            } else {
                posterPath = '/static/img/nanPosterBig.jpg'
            }

            var movieId = movie['MovieId'];
            var cardId = 'card_' + movieId;

            $("#wishlist-result").append(
                $.parseHTML(
                    '<div class="col s12 m6 l4" id="' + cardId+ '">' +
                        '<div class="card horizontal z-depth-0">' +
                            '<div class="card-image">' +
                                '<img class="series-poster search-object-series" src="' + posterPath + '" alt="Poster for movie: ' +
                                    movie['Title'] + '" onclick="openMovie(' + movieId + ')">' +
                            '</div>' +
                            '<div class="card-stacked truncate">' +
                                '<div class="card-content">' +
                                    '<a class="truncate content-key search-object-series black-text" onclick="openMovie(' + movieId + ')">' +
                                    movie['Title'] +
                                    '</a>' +
                                    '<span>' + movie['Year'] + '</span>' +
                                '</div>' +
                                '<div class="card-action">' +
                                    '<a class="search-object-series red-text" onclick="removeFromWishlist(' + movieId + ')">' + lang['HISTORY_REMOVE'] + '</a>' +
                                '</div>' +
                            '</div>' +
                        '</div>' +
                    '</div>'
                )
            );
        }, timeout += 25);
    });
    $('.megatest').matchHeight({
        byRow: true,
        property: 'height',
        target: $('.responsive-img'),
        remove: false
    });
}

function openMovie(movieId) {
    location.href = 'movies/?id=' + movieId;
}

function removeFromWishlist(movieId) {
    console.log('removed', movieId);
    eventbus.send("database_remove_wishlist", movieId, function (error, reply) {
        if (reply['body']['updated'] != null) {
            var id = 'card_' + movieId;
            console.log('REMOVED', id);
            $(document.getElementById(id)).remove();
        }
    });
}