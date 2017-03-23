fallback.ready(['jQuery', 'EventBus'], function () {
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
                console.log(data);
                //addTableHead(lang);
                addTableData(data);
            });
        });
    };
    eventbus.onclose = function (json) {
        if (json.wasClean === false) { //connection lost
            if (typeof Storage !== 'undefined') {
                var data = JSON.parse(localStorage.getItem("wishlist_data"));
                addTableData(data);
            }
        }
    };
});

function addTableHead(lang) {
    $("#wishlist-table").empty().append(
        '<tr>' +
        '<th class="table-row">' + lang['HISTORY_TITLE'] + '</th>' +
        '<th class="center">' + lang['HISTORY_DATE'] + '</th>' +
        '</tr>');
}

function addTableData(data) {
    var timeout = 0;
    $.each(data, function (i) {
        setTimeout(function () {
            var posterPath = "";
            var movie = data[i];
            console.log(movie['Title']);
            if (movie['Image'] !== "") {
                posterPath = 'https://image.tmdb.org/t/p/w342' + movie['Image'];
            } else {
                posterPath = '/static/img/nanPosterBig.jpg'
            }

            $("#wishlist-result").append(
                $.parseHTML(
                    '<div class="col s6 m4 l2">' +
                    '<div class="card z-depth-2">' +
                    '<div class="card-image">' +
                    '<img src="' + posterPath + '" alt="Poster for movie: ' +
                    movie['Title'] + '">' +
                    '</div>' +
                    '</div>' +
                    '</div>'
                )
            );
        }, timeout += 25);
    });
}