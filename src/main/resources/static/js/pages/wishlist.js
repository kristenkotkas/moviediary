fallback.ready(['jQuery', 'SockJS', 'EventBus'], function () {
    var eventbus = new EventBus("/eventbus");
    eventbus.onopen = function () {
        var lang;
        eventbus.send("translations", getCookie("lang"), function (error, reply) {
            lang = reply.body;
            console.log(lang);
            eventbus.send("database_get_wishlist",
                {}, function (error, reply) {
                    var data = reply.body['rows'];
                    console.log(data);
                    //addTableHead(lang);
                    addTableData(data, lang);
                });
        });
    };
});

function addTableHead(lang) {
    $("#wishlist-table").empty().append(
        '<tr>' +
        '<th class="table-row">' + lang['HISTORY_TITLE'] + '</th>' +
        '<th class="center">' + lang['HISTORY_DATE'] + '</th>' +
        '</tr>');
}

function addTableData(data, lang) {
    var timeout = 100;
    $.each(data, function (i) {
        setTimeout(function () {
            var posterPath = "";
            var movie = data[i];
            console.log(movie['Title']);
            if (movie['Image'] != "") {
                posterPath = 'https://image.tmdb.org/t/p/w342' + movie['Image'];
            } else {
                posterPath = '/static/img/nanPosterSmall.jpg'
            }

            $("#wishlist-result").append(
                $.parseHTML(
                    '<div class="col s6 m4 l2">' +
                    '<div class="card card-small z-depth-2">' +
                    '<div class="card-image">' +
                    '<img src="' + posterPath + '" alt="Poster for movie: ' +
                    movie['Title'] + '" class="search-image">' +
                    '</div>' +
                    '</div>' +
                    '</div>'
                )
            );
        }, timeout += 25);
    });
}